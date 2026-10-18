package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.Locale;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.LinkedMultiValueMap;

@SpringBootTest(properties={"spring.config.import=",
 "spring.datasource.url=jdbc:h2:mem:relatorio_bugs;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"})
@Import(TestAccounts.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RelatorioBugsIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired EspacoRepository spaces;
 @Autowired PersonaRepository personas;
 @Autowired TabelaPrecoRepository prices;
 @Autowired SolicitacaoPublicaRepository requests;
 @Autowired EmpreendedorRepository people;
 @Autowired Clock clock;
 @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
 Espaco room;
 LocalDate date;
 @BeforeEach void fixtures(){
  requests.deleteAll();people.deleteAll();spaces.deleteAll();
  date=LocalDate.now(clock).plusDays(2);
  room=new Espaco();room.nomeEspaco="Sala São José";room.capacidade=20;room.ativo=true;room.disponibilidade=true;spaces.saveAndFlush(room);
  if(prices.findByAnoAndQuantidadeTurnosAndPercentualAndAtivoTrue(date.getYear(),1,100).isEmpty()){
   var p=new TabelaPreco();p.ano=date.getYear();p.quantidadeTurnos=1;p.percentual=100;p.valor=new BigDecimal("100.00");p.ativo=true;prices.saveAndFlush(p);
  }
 }
 LinkedMultiValueMap<String,String> fields(){
  var p=new LinkedMultiValueMap<String,String>();
  p.set("espacoId",room.id.toString());p.set("personaId",personas.findByCodigo("P6").orElseThrow().id.toString());
  p.set("data",date.toString());p.set("horaInicio","09:00");p.set("horaFim","10:00");
  p.set("nome","João D’Ávila");p.set("cnpj","12345678000190");p.set("email","joao@example.com");
  p.set("telefone","+55 (48) 99999-1234");p.set("empresa","Instituição");p.set("cidade","São José");p.set("titulo","Reunião");p.set("objetivo","Conectar");p.set("numeroParticipantes","10");return p;
 }
 MockHttpServletRequestBuilder summary(LinkedMultiValueMap<String,String> p){return post("/reservas/resumo").with(csrf()).locale(Locale.forLanguageTag("pt-BR")).params(p);}
 void dateRendered(String html){assertTrue(html.matches("(?s).*<input[^>]*id=\"data\"[^>]*value=\""+date+"\"[^>]*>.*"),"date input must contain ISO date, independent of locale");}
 @ParameterizedTest @CsvSource({"nome,123134142","nome,João 123","email,11231231@a","email,joao@example..com","telefone,123123adwdawd","telefone,123"})
 void invalidContactReturnsSameFormWithoutPersisting(String field,String value)throws Exception{
  long before=requests.count();var p=fields();p.set(field,value);
  var result=mvc.perform(summary(p)).andExpect(status().isBadRequest()).andExpect(view().name("public/reserva-form"))
   .andExpect(model().attributeHasFieldErrors("form",field)).andReturn();
  dateRendered(result.getResponse().getContentAsString());assertEquals(before,requests.count());
 }
 @Test void agendaSelectionRendersIsoDateAndCapacityEvenInPortuguese()throws Exception{
  var html=mvc.perform(get("/reserva").locale(Locale.forLanguageTag("pt-BR")).param("data",date.toString()).param("espacoId",room.id.toString()).param("horaInicio","09:00").param("horaFim","10:00"))
   .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  dateRendered(html);assertTrue(html.contains("20 pessoas"));assertTrue(html.contains("max=\""+LocalDate.of(LocalDate.now(clock).getYear()+1,12,31)+"\""));
 }
 @Test void reviewAllowsEditingWithoutDependingOnBrowserHistory()throws Exception{
  var result=mvc.perform(summary(fields())).andExpect(status().isOk()).andExpect(view().name("public/reserva-resumo")).andReturn();
  var session=(MockHttpSession)result.getRequest().getSession();
  var html=mvc.perform(get("/reserva").param("editar","true").session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  dateRendered(html);assertTrue(org.springframework.web.util.HtmlUtils.htmlUnescape(html).contains("João D’Ávila"));assertTrue(html.contains("joao@example.com"));
 }
 @Test void invalidDateIsFriendlyAndKeepsOtherFields()throws Exception{
  var p=fields();p.set("data","134421-03-12");
  var html=mvc.perform(summary(p)).andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
  assertFalse(html.contains("Failed to convert"));assertFalse(html.contains("java.time"));assertTrue(html.contains("joao@example.com"));
 }
 @Test void noRequestDoesNotOfferAnUnusableCodeForm()throws Exception{
  var html=mvc.perform(get("/solicitacoes/confirmar")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  assertFalse(html.contains("name=\"code\""));assertTrue(html.contains("Nenhuma solicitação"));
 }
 @Test void localSubmissionExplainsManualReviewWithoutRequestingACode()throws Exception{
  var result=mvc.perform(summary(fields())).andExpect(status().isOk()).andReturn();
  var session=(MockHttpSession)result.getRequest().getSession();
  mvc.perform(post("/reservas/solicitar").with(csrf()).session(session)).andExpect(status().is3xxRedirection());
  var html=mvc.perform(get("/solicitacoes/confirmar").session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  assertFalse(html.contains("name=\"code\""));assertTrue(html.contains("conferência manual"));
  assertEquals(0,people.findByCnpj("12345678000190").stream().count());
 }
 @Test void activeCodeIsShownButNeverDisclosedAndExpiredCodeIsHidden()throws Exception{
  var result=mvc.perform(summary(fields())).andExpect(status().isOk()).andReturn();var session=(MockHttpSession)result.getRequest().getSession();
  mvc.perform(post("/reservas/solicitar").with(csrf()).session(session)).andExpect(status().is3xxRedirection());
  var s=requests.findByReferencia((String)session.getAttribute("request.reference")).orElseThrow();s.codigoHash="private-code-hash-fixture";s.codigoExpiraEm=LocalDateTime.now(clock).plusMinutes(10);requests.saveAndFlush(s);
  var html=mvc.perform(get("/solicitacoes/confirmar").session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  assertTrue(html.contains("name=\"code\""));assertFalse(html.contains(s.codigoHash));
  s.codigoExpiraEm=LocalDateTime.now(clock).minusSeconds(1);requests.saveAndFlush(s);
  html=mvc.perform(get("/solicitacoes/confirmar").session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertFalse(html.contains("name=\"code\""));
 }
 @Test @WithMockUser(username="fixture-admin",roles="ADMIN")
 void invalidCrudContactRollsBackAndAllowsCorrection()throws Exception{
  var p=fields();p.set("nome","João 123");
  mvc.perform(post("/admin/cadastros/empreendedores/salvar").with(csrf()).params(p)).andExpect(status().isBadRequest()).andExpect(view().name("admin/crud/form"));
  assertTrue(people.findByCnpj("12345678000190").isEmpty());p.set("nome","João D’Ávila");
  mvc.perform(post("/admin/cadastros/empreendedores/salvar").with(csrf()).params(p)).andExpect(status().is3xxRedirection());assertTrue(people.findByCnpj("12345678000190").isPresent());
 }
 @Test @WithMockUser(username="fixture-admin",roles="ADMIN")
 void invalidEditPreservesPersistedContactAndCanBeCorrected()throws Exception{
  var p=fields();mvc.perform(post("/admin/cadastros/empreendedores/salvar").with(csrf()).params(p)).andExpect(status().is3xxRedirection());
  var person=people.findByCnpj("12345678000190").orElseThrow();p.set("id",person.id.toString());p.set("nome","João 123");p.set("email","novo@example.com");
  mvc.perform(post("/admin/cadastros/empreendedores/salvar").with(csrf()).params(p)).andExpect(status().isBadRequest());
  var unchanged=people.findById(person.id).orElseThrow();assertEquals(person.nome,unchanged.nome);assertEquals(person.email,unchanged.email);
  p.set("nome","Maria-José");mvc.perform(post("/admin/cadastros/empreendedores/salvar").with(csrf()).params(p)).andExpect(status().is3xxRedirection());
  assertEquals("Maria-José",people.findById(person.id).orElseThrow().nome);
 }

 @Test @WithMockUser(username="fixture-admin",roles="ADMIN")
 void historicalInvalidContactRemainsReadableAndCanBeRejectedWithoutRewritingIt()throws Exception{
  var result=mvc.perform(summary(fields())).andExpect(status().isOk()).andReturn();var session=(MockHttpSession)result.getRequest().getSession();
  mvc.perform(post("/reservas/solicitar").with(csrf()).session(session)).andExpect(status().is3xxRedirection());
  var original=requests.findByReferencia((String)session.getAttribute("request.reference")).orElseThrow();
  jdbc.update("update solicitacao_publica set nome=? where id=?","Contato legado 123",original.id);
  mvc.perform(post("/admin/solicitacoes/"+original.id+"/recusar").with(csrf())).andExpect(status().is3xxRedirection());
  var kept=requests.findById(original.id).orElseThrow();assertEquals("RECUSADA",kept.estado);assertEquals("Contato legado 123",kept.form.nome);
 }

}

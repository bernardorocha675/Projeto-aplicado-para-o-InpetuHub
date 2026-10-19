package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.inpetu.hub.config.LocalAccounts;
import br.com.inpetu.hub.config.PrivateAccountInitializer;
import br.com.inpetu.hub.enums.Perfil;
import br.com.inpetu.hub.repository.*;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties={
 "spring.config.import=",
 "spring.datasource.url=jdbc:h2:mem:public_demo_v04;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
 "spring.datasource.password=", "app.accounts-file=arquivo-inexistente.properties",
 "app.public-demo-accounts=true", "app.seed-demo=true"})
@ActiveProfiles("local")
@AutoConfigureMockMvc
@Transactional
class PublicDemoLoginTest {
 static Path key;
 @DynamicPropertySource static void privateKey(DynamicPropertyRegistry registry) throws Exception {
  key=Files.createTempFile("inpetu-v04-key-",".key");
  byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);
  Files.writeString(key,Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
  registry.add("app.security.key-file",()->key.toString());
 }
 @AfterAll static void clean() throws Exception {if(key!=null)Files.deleteIfExists(key);}
 @Autowired MockMvc mvc;
 @Autowired ColaboradorRepository users;
 @Autowired EmpreendedorRepository people;
 @Autowired ReservaRepository reservations;
 @Autowired PrivateAccountInitializer initializer;
 @Autowired PasswordEncoder encoder;

 @Test void fiveAccountsAuthenticateWithoutAnExternalPasswordsFile() throws Exception {
  assertEquals(5,users.count());
  for(var account:LocalAccounts.all()){
   var result=mvc.perform(post("/login").with(csrf()).param("username",account.login()).param("password",account.password()))
       .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin")).andReturn();
   var session=(org.springframework.mock.web.MockHttpSession)result.getRequest().getSession(false);
   mvc.perform(get("/admin/config").session(session)).andExpect(status().is(account.role()==Perfil.ADMIN?200:403));
  }
 }
 @Test void publicDemoCredentialsAreRestoredWhenApplicationStartsAgain() throws Exception {
  var account=users.findByLogin("bernardo").orElseThrow();
  account.senha=encoder.encode("synthetic-change-only");account.ativo=false;account.perfil=Perfil.COLABORADOR;users.saveAndFlush(account);
  initializer.run(new DefaultApplicationArguments());
  var restored=users.findByLogin("bernardo").orElseThrow();
  assertTrue(restored.ativo);assertEquals(Perfil.ADMIN,restored.perfil);
  assertTrue(encoder.matches(LocalAccounts.all().get(1).password(),restored.senha));
 }
 @Test void newDesktopDatabaseStartsWithNoBusinessDemoRecords(){
  assertEquals(0,people.count());assertEquals(0,reservations.count());
 }

 @Test void emptyDesktopCalendarDoesNotClaimThatEveryDayIsOccupied() throws Exception {
  mvc.perform(get("/disponibilidade").param("mes","2026-10"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.dias.length()").value(31))
      .andExpect(jsonPath("$.dias[*].estado").value(
          org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("SEM_ESPACOS"))))
      .andExpect(jsonPath("$.dias[0].espacos").isEmpty());
 }
 @Test void restoredPublicNavigationStaysInsideTheApplication() throws Exception {
  for(String path:new String[]{"/","/agenda","/reserva","/login"}){
   var html=mvc.perform(get(path)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
   assertLocalHeader(html,path);
  }
 }
 @Test void restoredManagementScreensKeepLocalNavigationAfterLogin() throws Exception {
  var result=mvc.perform(post("/login").with(csrf()).param("username","bernardo").param("password",LocalAccounts.all().stream().filter(a->a.login().equals("bernardo")).findFirst().orElseThrow().password()))
    .andExpect(redirectedUrl("/admin")).andReturn();
  var session=(org.springframework.mock.web.MockHttpSession)result.getRequest().getSession(false);
  for(String path:new String[]{"/admin","/admin/reservas","/admin/solicitacoes","/admin/cadastros/empreendedores","/admin/cadastros/espacos","/admin/cadastros/atendimentos","/admin/cadastros/visitas","/admin/config","/admin/config/cotas","/admin/config/auditoria","/conta/senha"}){
   var html=mvc.perform(get(path).session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
   assertLocalHeader(html,path);
   if(path.startsWith("/admin"))assertTrue(html.contains("aria-label=\"Menu interno\""),path);
  }
 }
 private void assertLocalHeader(String html,String path){
  int start=html.indexOf("<header"),end=html.indexOf("</header>",start);
  assertTrue(start>=0&&end>start,path+" perdeu o cabeçalho");
  var top=html.substring(start,end);
  // Desde a 0.5.1 o menu é o do site oficial: externos só do domínio oficial, em nova aba.
  var links=java.util.regex.Pattern.compile("href=\"(https?://[^\"]+)\"([^>]*)").matcher(top);
  while(links.find()){
   assertTrue(links.group(1).startsWith("https://inpetuhub.sites.ufsc.br/"),path+" aponta para fora do site oficial: "+links.group(1));
   assertTrue(links.group(2).contains("target=\"_blank\""),path+" deve abrir o site oficial em outra aba");
  }
  for(String target:new String[]{"/","/agenda","/admin"})assertTrue(top.contains("href=\""+target+"\""),path+" perdeu a navegação local");
 }

}

package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.inpetu.hub.entity.Espaco;
import br.com.inpetu.hub.repository.EspacoRepository;
import br.com.inpetu.hub.observability.OperationalEvents;
import br.com.inpetu.hub.observability.SafeLogEncoder;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.config.import=",
    "spring.datasource.url=jdbc:h2:mem:espacos_cadastro;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
@Import(TestAccounts.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "fixture-admin", roles = "ADMIN")
class EspacosCadastroIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired EspacoRepository espacos;

  @BeforeEach
  void limpar() {
    espacos.deleteAll();
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "0.89", "1.5", "1,5", "2147483648", "abc", ""})
  void rejeitaCapacidadeInvalidaSemCriarRegistro(String capacidade) throws Exception {
    var result = mvc.perform(post("/admin/cadastros/espacos/salvar").with(csrf())
            .param("nomeEspaco", "Sala de teste")
            .param("capacidade", capacidade).param("disponibilidade", "on"))
        .andExpect(status().isBadRequest())
        .andExpect(view().name("admin/crud/form"))
        .andExpect(model().attributeExists("erro"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Salvar cadastro")))
        .andReturn();
    var registro = (Map<?, ?>) result.getModelAndView().getModel().get("registro");
    assertEquals(capacidade, registro.get("capacidade"));
    assertEquals("Sala de teste", registro.get("nomeEspaco"));
    assertEquals("true", registro.get("disponibilidade"));
    assertEquals("false", registro.get("ativo"));
    assertEquals(0, espacos.count());
  }

  @Test
  void aposZeroPermiteCorrigirESalvarSemReabrirCadastro() throws Exception {
    mvc.perform(post("/admin/cadastros/espacos/salvar").with(csrf())
            .param("nomeEspaco", "Nova sala").param("capacidade", "0"))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/admin/cadastros/espacos/salvar").with(csrf())
            .param("nomeEspaco", "Nova sala").param("capacidade", "25")
            .param("ativo", "on").param("disponibilidade", "on"))
        .andExpect(redirectedUrl("/admin/cadastros/espacos"));
    var salvo = espacos.findAll().get(0);
    assertEquals(25, salvo.capacidade);
    assertEquals("Nova sala", salvo.nomeEspaco);
    assertTrue(salvo.ativo);
    assertTrue(salvo.disponibilidade);
  }

  @Test
  void edicaoInvalidaPreservaBancoEPermiteCorrecao() throws Exception {
    var sala = new Espaco();
    sala.nomeEspaco = "Original";
    sala.capacidade = 10;
    sala.ativo = true;
    sala.disponibilidade = true;
    espacos.saveAndFlush(sala);
    mvc.perform(post("/admin/cadastros/espacos/salvar").with(csrf())
            .param("id", sala.id.toString()).param("nomeEspaco", "Alterada")
            .param("capacidade", "1.5"))
        .andExpect(status().isBadRequest())
        .andExpect(model().attribute("id", sala.id));
    var anterior = espacos.findById(sala.id).orElseThrow();
    assertEquals("Original", anterior.nomeEspaco);
    assertEquals(10, anterior.capacidade);
    assertTrue(anterior.ativo);
    mvc.perform(post("/admin/cadastros/espacos/salvar").with(csrf())
            .param("id", sala.id.toString()).param("nomeEspaco", "Alterada")
            .param("capacidade", "1"))
        .andExpect(redirectedUrl("/admin/cadastros/espacos"));
    var salvo = espacos.findById(sala.id).orElseThrow();
    assertEquals(1, espacos.count());
    assertEquals("Alterada", salvo.nomeEspaco);
    assertEquals(1, salvo.capacidade);
    assertFalse(salvo.ativo);
    assertFalse(salvo.disponibilidade);
  }

  @Test
  void formularioExigeInteiroPositivoEReenvioNaoFicaBloqueado() throws Exception {
    String html = mvc.perform(get("/admin/cadastros/espacos/editar"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    assertTrue(html.contains("data-repeat-submit=\"true\""));
    String input = java.util.regex.Pattern.compile("<input[^>]*name=\"capacidade\"[^>]*>")
        .matcher(html).results().map(java.util.regex.MatchResult::group).findFirst().orElseThrow();
    for (String attr : new String[] {"step=\"1\"", "min=\"1\"", "max=\"2147483647\"", "required"})
      assertTrue(input.contains(attr), input);
    mvc.perform(get("/admin/config/cadastros/precos/editar"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("step=\"0.01\"")));
  }

  @Test
  void erroDeValidacaoFicaNoLogSemCopiarFormulario() throws Exception {
    var logger = (Logger) LoggerFactory.getLogger(OperationalEvents.class);
    var capture = new ListAppender<ILoggingEvent>();
    capture.start();
    logger.addAppender(capture);
    try {
      mvc.perform(post("/admin/cadastros/espacos/salvar").with(csrf())
              .param("nomeEspaco", "conteudo-privado-nao-publicar").param("capacidade", "0"))
          .andExpect(status().isBadRequest());
      String log = capture.list.stream()
          .map(e -> new String(new SafeLogEncoder().encode(e), StandardCharsets.UTF_8))
          .collect(java.util.stream.Collectors.joining());
      assertTrue(log.contains("action=CADASTRO_INVALIDO"));
      assertTrue(log.contains("result=NEGADO"));
      assertFalse(log.contains("conteudo-privado-nao-publicar"));
    } finally {
      logger.detachAppender(capture);
      capture.stop();
    }
  }
}

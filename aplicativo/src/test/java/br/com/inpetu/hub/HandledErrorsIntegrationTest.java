package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.inpetu.hub.exception.RegraException;
import br.com.inpetu.hub.observability.*;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;

@SpringBootTest(properties={"spring.config.import=",
 "spring.datasource.url=jdbc:h2:mem:handled_errors;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestAccounts.class,HandledErrorsIntegrationTest.Routes.class})
@WithMockUser(username="fixture-admin",roles="ADMIN")
class HandledErrorsIntegrationTest {
 @TestConfiguration static class Routes {
  @Bean Failures testFailures(){return new Failures();}
 }
 @Controller static class Failures {
  @GetMapping("/test-logs/failure") String fail(){throw new IllegalStateException("sql-password-private@example.com");}
  @GetMapping("/test-logs/rule") String rule(){throw new RegraException("Confira o horário.");}
  @GetMapping("/test-logs/conflict") String conflict(){throw new DataIntegrityViolationException("sql-password-private@example.com");}
 }
 @Autowired MockMvc mvc;
 Logger logger;
 ListAppender<ILoggingEvent> capture;
 @BeforeEach void capture(){logger=(Logger)LoggerFactory.getLogger(OperationalEvents.class);capture=new ListAppender<>();capture.start();logger.addAppender(capture);}
 @AfterEach void stop(){logger.detachAppender(capture);capture.stop();}
 String log(){return capture.list.stream().map(e->new String(new SafeLogEncoder().encode(e),StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining());}
 @Test void unexpectedFailureReturnsFriendlyPageAndCorrelatesDetailedPrivateSafeLog() throws Exception {
  var response=mvc.perform(get("/test-logs/failure")).andExpect(status().isInternalServerError()).andExpect(view().name("error/500")).andReturn().getResponse();
  String id=response.getHeader("X-Request-ID"),text=log();
  assertNotNull(id);assertTrue(response.getContentAsString().contains(id));
  assertFalse(response.getContentAsString().contains("sql-password"));
  assertTrue(text.contains("REQUEST_FAILED"));assertTrue(text.contains("java.lang.IllegalStateException"));
  assertTrue(text.contains("HandledErrorsIntegrationTest$Failures.fail"));assertTrue(text.contains(id));
  assertFalse(text.contains("private@example.com"));
 }
 @Test void handledRuleIsWarnAndHttp400InsteadOfSilentSuccess() throws Exception {
  mvc.perform(get("/test-logs/rule")).andExpect(status().isBadRequest()).andExpect(view().name("erro"));
  assertTrue(log().contains("REQUEST_REJECTED"));assertTrue(log().contains("\"level\":\"WARN\""));
 }
 @Test void databaseConflictReturns409WithoutSqlOrPersonalData() throws Exception {
  var response=mvc.perform(get("/test-logs/conflict")).andExpect(status().isConflict()).andReturn().getResponse();
  assertFalse(response.getContentAsString().contains("sql-password"));assertFalse(log().contains("sql-password"));
  assertTrue(log().contains("DataIntegrityViolationException"));
 }
 @Test void invalidDateHasNoConversionMessageAndCreatesValidationLog() throws Exception {
  var response=mvc.perform(post("/reservas/resumo").with(csrf()).param("data","not-a-date"))
   .andExpect(status().isBadRequest()).andExpect(view().name("public/reserva-form")).andReturn().getResponse();
  assertFalse(response.getContentAsString().contains("Failed to convert"));
  assertFalse(response.getContentAsString().contains("java.time"));
  assertTrue(log().contains("VALIDATION_REJECTED"));assertTrue(log().contains("CONVERSION"));
  assertFalse(log().contains("not-a-date"));
 }
 @Test void missingRequiredParameterIs400AndMissingResourceRemains404() throws Exception {
  mvc.perform(post("/solicitacoes/confirmar").with(csrf())).andExpect(status().isBadRequest());
  mvc.perform(post("/agenda").with(csrf())).andExpect(status().isMethodNotAllowed())
   .andExpect(header().string("Allow",org.hamcrest.Matchers.containsString("GET")));
  mvc.perform(get("/test-logs/does-not-exist")).andExpect(status().isNotFound());
  assertTrue(log().contains("REQUEST_REJECTED"));assertFalse(log().contains("REQUEST_FAILED"));
 }
 @Test @WithMockUser(username="fixture-admin",roles="COLABORADOR")
 void authorizationRemains403() throws Exception {
  mvc.perform(get("/admin/config")).andExpect(status().isForbidden());
 }
}

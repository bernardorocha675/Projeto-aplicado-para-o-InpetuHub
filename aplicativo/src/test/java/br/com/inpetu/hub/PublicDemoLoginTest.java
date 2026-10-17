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
}

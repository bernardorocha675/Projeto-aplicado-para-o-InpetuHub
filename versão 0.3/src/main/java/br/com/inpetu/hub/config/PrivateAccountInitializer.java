package br.com.inpetu.hub.config;

import br.com.inpetu.hub.entity.Colaborador;
import br.com.inpetu.hub.enums.Perfil;
import br.com.inpetu.hub.repository.*;
import br.com.inpetu.hub.security.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
@Order(0)
public class PrivateAccountInitializer implements ApplicationRunner {
  private final ColaboradorRepository users;
  private final ConfiguracaoRepository config;
  private final JdbcTemplate jdbc;
  private final PasswordEncoder encoder;
  private final AuditService audit;
  private final String file;

  public PrivateAccountInitializer(
      ColaboradorRepository users,
      ConfiguracaoRepository config,
      JdbcTemplate jdbc,
      PasswordEncoder encoder,
      AuditService audit,
      @Value("${app.accounts-file:.secrets/accounts.properties}") String file) {
    this.users = users;
    this.config = config;
    this.jdbc = jdbc;
    this.encoder = encoder;
    this.audit = audit;
    this.file = file;
  }

  public static List<Colaborador> read(Path file, PasswordEncoder encoder) {
    try {
      if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) > 65536)
        throw new IllegalArgumentException();
      Properties p = new Properties();
      try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
        p.load(reader);
      }
      var result = new ArrayList<Colaborador>();
      var logins = new HashSet<String>();
      Set<String> consumed = new HashSet<>();
      for (int i = 1; i <= 100 && p.containsKey("account." + i + ".login"); i++) {
        String prefix = "account." + i + ".";
        Colaborador c = new Colaborador();
        c.nome = p.getProperty(prefix + "name", "").trim();
        c.login = p.getProperty(prefix + "login", "").trim();
        String password = p.getProperty(prefix + "password", "");
        c.perfil = Perfil.valueOf(p.getProperty(prefix + "role", ""));
        if (!c.login.matches("[A-Za-z0-9._@+-]{4,120}")
            || !logins.add(c.login.toLowerCase(Locale.ROOT))
            || c.nome.isBlank()
            || c.nome.length() > 120
            || password.length() < 16
            || password.length() > 64
            || password.getBytes(StandardCharsets.UTF_8).length > 72)
          throw new IllegalArgumentException();
        c.senha = encoder.encode(password);
        c.email = "";
        c.ativo = true;
        c.securityVersion = 1;
        result.add(c);
        for (String key : List.of("name", "login", "password", "role")) consumed.add(prefix + key);
      }
      if (result.isEmpty()
          || result.stream().noneMatch(c -> c.perfil == Perfil.ADMIN)
          || !consumed.containsAll(p.stringPropertyNames())) throw new IllegalArgumentException();
      return result;
    } catch (Exception ex) {
      throw new IllegalStateException(
          "Arquivo privado de contas inválido. Use nomes, logins únicos, senhas de 16–64 caracteres"
              + " e ao menos um ADMIN. Valores não foram registrados.");
    }
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    config.bloquear();
    if (Boolean.TRUE.equals(
        jdbc.queryForObject("select concluido from bootstrap_seguro where id=1", Boolean.class)))
      return;
    List<Colaborador> input = read(Path.of(file), encoder); // validar tudo antes de escrever.
    // Em migração, revogar todas as contas antigas; ativar somente as provisionadas em privado.
    for (Colaborador old : users.findAll()) {
      old.ativo = false;
      old.securityVersion++;
    }
    for (Colaborador item : input) {
      Colaborador c = users.findByLogin(item.login).orElseGet(Colaborador::new);
      c.nome = item.nome;
      c.login = item.login;
      c.email = "";
      c.senha = item.senha;
      c.perfil = item.perfil;
      c.ativo = true;
      c.securityVersion++;
      c.mfaSecret = null;
      c.mfaRecovery = null;
      c.mfaCounter = -1;
      users.save(c);
    }
    jdbc.update("update bootstrap_seguro set concluido=true where id=1");
    audit.record(
        null,
        "PROVISIONAMENTO",
        "contas",
        null,
        "OK",
        "Contas importadas de arquivo privado; contas anteriores revogadas.");
  }
}

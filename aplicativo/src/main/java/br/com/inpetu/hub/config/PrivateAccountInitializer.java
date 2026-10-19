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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
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
  private final SecretCrypto crypto;
  @Autowired private Environment environment;

  public PrivateAccountInitializer(
      ColaboradorRepository users,
      ConfiguracaoRepository config,
      JdbcTemplate jdbc,
      PasswordEncoder encoder,
      AuditService audit,
      SecretCrypto crypto,
      @Value("${app.accounts-file:.secrets/accounts.properties}") String file) {
    this.users = users;
    this.config = config;
    this.jdbc = jdbc;
    this.encoder = encoder;
    this.audit = audit;
    this.file = file;
    this.crypto = crypto;
  }

  private record PrivateAccount(String name, String login, String password, Perfil role) {}

  private static List<PrivateAccount> load(Path file) {
    try {
      if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) > 65536)
        throw new IllegalArgumentException();
      Properties p = new Properties();
      try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
        p.load(reader);
      }
      var result = new ArrayList<PrivateAccount>();
      var logins = new HashSet<String>();
      Set<String> consumed = new HashSet<>();
      for (int i = 1; i <= 100 && p.containsKey("account." + i + ".login"); i++) {
        String prefix = "account." + i + ".";
        String name = p.getProperty(prefix + "name", "").trim();
        String login = LoginNames.normalize(p.getProperty(prefix + "login", ""));
        String password = p.getProperty(prefix + "password", "");
        Perfil role = Perfil.valueOf(p.getProperty(prefix + "role", "").trim());
        if (!LoginNames.valid(login)
            || !logins.add(login)
            || name.isBlank()
            || name.length() > 120
            || password.length() < 8
            || password.length() > 64
            || password.getBytes(StandardCharsets.UTF_8).length > 72)
          throw new IllegalArgumentException();
        result.add(new PrivateAccount(name, login, password, role));
        for (String key : List.of("name", "login", "password", "role")) consumed.add(prefix + key);
      }
      if (result.isEmpty()
          || result.stream().noneMatch(c -> c.role == Perfil.ADMIN)
          || !consumed.containsAll(p.stringPropertyNames())) throw new IllegalArgumentException();
      return result;
    } catch (Exception ex) {
      throw new IllegalStateException(
          "Arquivo privado de contas inválido. Use nomes, logins únicos, senhas de 8–64 caracteres"
              + " e ao menos um ADMIN. Valores não foram registrados.");
    }
  }

  /** Mantém o formato público usado pelos comandos offline de conferência e recuperação. */
  public static List<Colaborador> read(Path file, PasswordEncoder encoder) {
    return load(file).stream()
        .map(
            item -> {
              var c = new Colaborador();
              c.nome = item.name;
              c.login = item.login;
              c.senha = encoder.encode(item.password);
              c.perfil = item.role;
              c.email = "";
              c.ativo = true;
              c.securityVersion = 1;
              return c;
            })
        .toList();
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    boolean publicDemo = environment != null && environment.matchesProfiles("local")
        && !environment.matchesProfiles("prod")
        && environment.getProperty("app.public-demo-accounts", Boolean.class, true);
    config.bloquear();
    boolean first =
        !Boolean.TRUE.equals(
            jdbc.queryForObject(
                "select concluido from bootstrap_seguro where id=1", Boolean.class));
    Path source = Path.of(file);
    if (!publicDemo && !first && !Files.exists(source, LinkOption.NOFOLLOW_LINKS)) return;
    List<PrivateAccount> input = publicDemo
        ? LocalAccounts.all().stream().map(a -> new PrivateAccount(a.name(),a.login(),a.password(),a.role())).toList()
        : load(source);
    if (environment != null && environment.matchesProfiles("prod")
        && input.stream().anyMatch(a -> LocalAccounts.all().stream().anyMatch(d -> d.password().equals(a.password))))
      throw new IllegalStateException("Senhas publicas de demonstracao recusadas em producao."); // Validar tudo antes de alterar o banco.
    if (first) {
      for (Colaborador old : users.findAll()) {
        old.ativo = false;
        old.securityVersion++;
      }
    }
    int changed = 0;
    for (PrivateAccount item : input) {
      String signature =
          crypto.fingerprint(
              "basic-account:"
                  + item.login
                  + "\0"
                  + item.name
                  + "\0"
                  + item.password
                  + "\0"
                  + item.role.name());
      var imported =
          jdbc.queryForList(
              "select assinatura from importacao_conta where login=?", String.class, item.login);
      var existing = users.findByLoginIgnoreCase(item.login);
      if (!first && existing.isPresent() && imported.contains(signature)
          && (!publicDemo || (existing.get().ativo && existing.get().perfil == item.role
              && encoder.matches(item.password, existing.get().senha)))) continue;
      Colaborador c = existing.orElseGet(Colaborador::new);
      c.nome = item.name;
      c.login = item.login;
      if (c.email == null) c.email = "";
      c.senha = encoder.encode(item.password);
      c.perfil = item.role;
      c.ativo = true;
      c.securityVersion++;
      users.saveAndFlush(c);
      if (imported.isEmpty())
        jdbc.update(
            "insert into importacao_conta(login,assinatura) values(?,?)", item.login, signature);
      else
        jdbc.update(
            "update importacao_conta set assinatura=? where login=?", signature, item.login);
      changed++;
    }
    jdbc.update("update bootstrap_seguro set concluido=true where id=1");
    if (changed > 0)
      audit.record(
          null,
          first ? "PROVISIONAMENTO" : "CONTAS_ATUALIZADAS",
          "contas",
          null,
          "OK",
          "Contas do arquivo privado atualizadas: " + changed + ".");
  }
}

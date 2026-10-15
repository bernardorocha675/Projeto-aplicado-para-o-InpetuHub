package br.com.inpetu.hub;

import br.com.inpetu.hub.entity.Colaborador;
import br.com.inpetu.hub.enums.Perfil;
import br.com.inpetu.hub.repository.ColaboradorRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
public class TestAccounts {
  @Bean
  ApplicationRunner testAccounts(ColaboradorRepository repo, PasswordEncoder encoder) {
    return args -> {
      if (repo.findByLogin("fixture-admin").isEmpty()) {
        var c = new Colaborador();
        c.nome = "Conta sintética de teste";
        c.login = "fixture-admin";
        c.email = "fixture@example.com";
        c.senha = encoder.encode("Test-only-Password-2026!");
        c.perfil = Perfil.ADMIN;
        c.ativo = true;
        c.securityVersion = 1;
        repo.save(c);
      }
    };
  }
}

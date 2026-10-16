package br.com.inpetu.hub.config;

import br.com.inpetu.hub.enums.Perfil;
import java.util.List;

/** Contas PUBLICAS da demonstracao local. Nunca reutilizar estas senhas em outros servicos. */
public final class LocalAccounts {
  private LocalAccounts() {}
  public record Account(String name, String login, String password, Perfil role) {}
  public static List<Account> all() {
    return List.of(
        new Account("Milena", "milena", "milena040", Perfil.COLABORADOR),
        new Account("Bernardo", "bernardo", "bernardo040", Perfil.ADMIN),
        new Account("João F", "joaof", "joaof040", Perfil.COLABORADOR),
        new Account("João E", "joaoe", "joaoe040", Perfil.COLABORADOR),
        new Account("Guilherme", "guilherme", "guilherme040", Perfil.COLABORADOR));
  }
}

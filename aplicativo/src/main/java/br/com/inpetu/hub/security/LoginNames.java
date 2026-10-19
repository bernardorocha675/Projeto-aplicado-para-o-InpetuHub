package br.com.inpetu.hub.security;

import java.text.Normalizer;
import java.util.Locale;

/** Normaliza somente o usuário; a senha permanece exatamente como foi digitada. */
public final class LoginNames {
  private LoginNames() {}

  /** Formato único de usuário para contas criadas pela tela, pelo arquivo privado ou pela manutenção. */
  public static final String PATTERN = "[a-z0-9._@+-]{4,120}";

  public static boolean valid(String normalized) {
    return normalized != null && normalized.matches(PATTERN);
  }

  public static String normalize(String value) {
    if (value == null) return "";
    return Normalizer.normalize(value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .replaceAll("[\\p{Z}\\s]+", "")
        .toLowerCase(Locale.ROOT);
  }
}

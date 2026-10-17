package br.com.inpetu.hub.security;

import java.text.Normalizer;
import java.util.Locale;

/** Normaliza somente o usuário; a senha permanece exatamente como foi digitada. */
public final class LoginNames {
  private LoginNames() {}

  public static String normalize(String value) {
    if (value == null) return "";
    return Normalizer.normalize(value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .replaceAll("[\\p{Z}\\s]+", "")
        .toLowerCase(Locale.ROOT);
  }
}

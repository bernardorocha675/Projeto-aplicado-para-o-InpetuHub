package br.com.inpetu.hub.conta;

import java.text.Normalizer;
import java.util.Locale;

/** Normaliza só o usuário (sem acentos, sem espaços, minúsculo); a senha fica exatamente como foi digitada. */
public final class NomesDeLogin {

    /** Formato único de usuário, o mesmo do sistema antigo. */
    public static final String FORMATO = "[a-z0-9._@+-]{4,120}";

    private NomesDeLogin() {
    }

    public static boolean valido(String normalizado) {
        return normalizado != null && normalizado.matches(FORMATO);
    }

    public static String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[\\p{Z}\\s]+", "")
                .toLowerCase(Locale.ROOT);
    }
}

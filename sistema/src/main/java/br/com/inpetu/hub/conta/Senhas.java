package br.com.inpetu.hub.conta;

import java.nio.charset.StandardCharsets;

import org.mindrot.jbcrypt.BCrypt;

/** Senhas guardadas só como BCrypt (custo 12, como no antigo). O BCrypt só lê os primeiros 72 bytes. */
public final class Senhas {

    static final String TAMANHO = "Use uma nova senha de 8 a 64 caracteres (até 72 bytes).";
    private static final int CUSTO = 12;
    /** Conferido quando o usuário não existe, para a resposta levar o mesmo tempo. */
    private static final String FALSO = BCrypt.hashpw("conta-inexistente", BCrypt.gensalt(CUSTO));

    private Senhas() {
    }

    public static String hash(String senha) {
        return BCrypt.hashpw(senha, BCrypt.gensalt(CUSTO));
    }

    public static boolean confere(String senha, String hash) {
        if (senha == null || hash == null || !hash.startsWith("$2")) {
            return false;
        }
        try {
            return BCrypt.checkpw(senha, hash);
        } catch (IllegalArgumentException hashInvalido) {
            return false;
        }
    }

    /** Gasta o mesmo tempo de uma conferência, sem conta. */
    static void conferirNada(String senha) {
        confere(senha == null ? "" : senha, FALSO);
    }

    /** O problema da nova senha, ou null se ela serve. */
    static String problemaDaNova(String nova) {
        if (nova == null || nova.length() < 8 || nova.length() > 64 || nova.getBytes(
                StandardCharsets.UTF_8).length > 72) {
            return TAMANHO;
        }
        return null;
    }
}

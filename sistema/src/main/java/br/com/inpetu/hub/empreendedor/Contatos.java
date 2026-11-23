package br.com.inpetu.hub.empreendedor;

import br.com.inpetu.hub.comum.RegraException;

/**
 * Formatos de contato do pedido público (e, depois, do cadastro). Telefone pela regra de dígitos de 21/10/2026, sem
 * libphonenumber (decisão 3b): Brasil com DDD, de 10 a 15 dígitos; outro país com + e o código, de 8 a 15.
 */
public final class Contatos {

    public static final String NOME = "[\\p{L}][\\p{L}\\p{M} .'’-]*";
    public static final String EMAIL = "[^\\s@]+@(?:[\\p{L}\\p{N}](?:[\\p{L}\\p{N}-]{0,61}[\\p{L}\\p{N}])?\\.)+"
            + "[\\p{L}]{2,63}";
    public static final String MENSAGEM_TELEFONE = "Informe um telefone válido: no Brasil, com DDD (10 a 15 dígitos);"
            + " de outro país, com + e o código do país (8 a 15 dígitos).";

    private Contatos() {
    }

    public static boolean nomeValido(String nome) {
        return nome != null && nome.matches(NOME);
    }

    public static boolean emailValido(String email) {
        return email != null && email.matches(EMAIL);
    }

    /** O telefone pronto para gravar: no Brasil, (DD) NNNNN-NNNN quando tem 10 ou 11 dígitos. */
    public static String telefone(String valor) {
        String t = valor == null ? "" : valor.strip().replaceAll("\\s+", " ");
        if (!t.matches("\\+?[0-9 ().-]+")) {
            throw new RegraException(MENSAGEM_TELEFONE);
        }
        String d = t.replaceAll("[^0-9]", "");
        if (t.startsWith("+")) {
            if (d.length() < 8 || d.length() > 15) {
                throw new RegraException(MENSAGEM_TELEFONE);
            }
            return t;
        }
        return switch (d.length()) {
            case 10 -> "(" + d.substring(0, 2) + ") " + d.substring(2, 6) + "-" + d.substring(6);
            case 11 -> "(" + d.substring(0, 2) + ") " + d.substring(2, 7) + "-" + d.substring(7);
            case 12, 13, 14, 15 -> t;
            default -> throw new RegraException(MENSAGEM_TELEFONE);
        };
    }
}

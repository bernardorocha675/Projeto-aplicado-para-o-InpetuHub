package br.com.inpetu.hub.empreendedor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import br.com.inpetu.hub.comum.RegraException;

/**
 * Documento do empreendedor: CNPJ, CPF, documento estrangeiro ou nenhum (decisão de 22/10/2026). Só a quantidade de
 * dígitos é conferida, sem dígito verificador. As mensagens não repetem o nome do campo (a tela já mostra).
 */
public final class Documentos {

    public static final String CNPJ = "CNPJ";
    public static final String CPF = "CPF";
    public static final String ESTRANGEIRO = "ESTRANGEIRO";
    public static final String NENHUM = "NENHUM";

    /** Tipos na ordem da tela, com o rótulo. */
    public static final Map<String, String> TIPOS = tipos();

    /** Tipo e número prontos para gravar; sem documento, o número é nulo. */
    public record Documento(String tipo, String numero) {
    }

    private Documentos() {
    }

    private static Map<String, String> tipos() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(CNPJ, "CNPJ");
        m.put(CPF, "CPF");
        m.put(ESTRANGEIRO, "Documento estrangeiro");
        m.put(NENHUM, "Sem documento");
        return Collections.unmodifiableMap(m);
    }

    public static Documento normalizar(String tipo, String numero) {
        String n = numero == null ? "" : numero.strip();
        // Sem tipo: vale como CNPJ (o único documento até a 0.5.6) ou, sem número, como nenhum.
        String t = tipo == null || tipo.isBlank() ? (n.isEmpty() ? NENHUM : CNPJ) : tipo.strip();
        if (!TIPOS.containsKey(t)) {
            throw new RegraException("Escolha um tipo de documento da lista.");
        }
        if (t.equals(NENHUM)) {
            if (!n.isEmpty()) {
                throw new RegraException("Com “Sem documento”, deixe o número em branco.");
            }
            return new Documento(NENHUM, null);
        }
        if (n.isEmpty()) {
            throw new RegraException("Informe o número do " + (t.equals(ESTRANGEIRO) ? "documento estrangeiro" : t)
                    + ".");
        }
        return switch (t) {
            case CNPJ -> new Documento(CNPJ, digitos(n, 14, "Informe os 14 dígitos do CNPJ."));
            case CPF -> new Documento(CPF, digitos(n, 11, "Informe os 11 dígitos do CPF."));
            default -> {
                String e = n.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
                if (!e.matches("[A-Z0-9][A-Z0-9./-]{3,29}")) {
                    throw new RegraException("Use de 4 a 30 letras ou números (pontos, barras e hífens são aceitos).");
                }
                yield new Documento(ESTRANGEIRO, e);
            }
        };
    }

    /** Texto das telas, como “CPF 12345678909” ou “Sem documento”. */
    public static String exibir(String tipo, String numero) {
        if (numero == null || numero.isBlank()) {
            return TIPOS.get(NENHUM);
        }
        String t = tipo == null || tipo.isBlank() ? CNPJ : tipo;
        return TIPOS.getOrDefault(t, t) + " " + numero;
    }

    /** Aceita a pontuação comum (12.345.678/0001-90) e guarda só os dígitos. */
    private static String digitos(String n, int quantidade, String erro) {
        if (!n.matches("[0-9 ./-]+")) {
            throw new RegraException(erro);
        }
        String d = n.replaceAll("[^0-9]", "");
        if (d.length() != quantidade) {
            throw new RegraException(erro);
        }
        return d;
    }
}

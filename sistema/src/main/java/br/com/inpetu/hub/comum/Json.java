package br.com.inpetu.hub.comum;

import java.util.List;
import java.util.Map;

/**
 * JSON mínimo, só para escrever (sem biblioteca, decisão 3b): mapas, listas, textos, números, Sim/Não e nulo. Os textos
 * saem com {@code <}, {@code >} e {@code &} escapados, para nunca virarem HTML por engano.
 */
public final class Json {

    private Json() {
    }

    public static String escrever(Object valor) {
        StringBuilder saida = new StringBuilder();
        escrever(saida, valor);
        return saida.toString();
    }

    private static void escrever(StringBuilder saida, Object valor) {
        switch (valor) {
            case null -> saida.append("null");
            case String texto -> texto(saida, texto);
            case Boolean simNao -> saida.append(simNao);
            case Integer numero -> saida.append(numero);
            case Long numero -> saida.append(numero);
            case Map<?, ?> mapa -> {
                saida.append('{');
                boolean primeiro = true;
                for (Map.Entry<?, ?> item : mapa.entrySet()) {
                    saida.append(primeiro ? "" : ",");
                    texto(saida, String.valueOf(item.getKey()));
                    saida.append(':');
                    escrever(saida, item.getValue());
                    primeiro = false;
                }
                saida.append('}');
            }
            case List<?> lista -> {
                saida.append('[');
                for (int i = 0; i < lista.size(); i++) {
                    saida.append(i == 0 ? "" : ",");
                    escrever(saida, lista.get(i));
                }
                saida.append(']');
            }
            default -> throw new IllegalArgumentException("Tipo sem JSON: " + valor.getClass().getSimpleName());
        }
    }

    private static void texto(StringBuilder saida, String texto) {
        saida.append('"');
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"' -> saida.append("\\\"");
                case '\\' -> saida.append("\\\\");
                case '\n' -> saida.append("\\n");
                case '\r' -> saida.append("\\r");
                case '\t' -> saida.append("\\t");
                default -> {
                    if (c < 0x20 || c == '<' || c == '>' || c == '&' || c == ' ' || c == ' ') {
                        saida.append(String.format("\\u%04x", (int) c));
                    } else {
                        saida.append(c);
                    }
                }
            }
        }
        saida.append('"');
    }
}

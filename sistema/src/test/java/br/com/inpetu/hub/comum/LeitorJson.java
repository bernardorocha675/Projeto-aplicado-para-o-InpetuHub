package br.com.inpetu.hub.comum;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Leitor de JSON mínimo, só para os testes: objetos viram Map, listas viram List e números viram Long. */
public final class LeitorJson {

    private final String texto;
    private int posicao;

    private LeitorJson(String texto) {
        this.texto = texto;
    }

    public static Object ler(String texto) {
        LeitorJson leitor = new LeitorJson(texto);
        Object valor = leitor.valor();
        leitor.espacos();
        if (leitor.posicao != texto.length()) {
            throw new IllegalArgumentException("Sobrou texto depois do JSON na posição " + leitor.posicao);
        }
        return valor;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> objeto(Object valor) {
        return (Map<String, Object>) valor;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> lista(Object valor) {
        return (List<Object>) valor;
    }

    private Object valor() {
        espacos();
        char c = texto.charAt(posicao);
        if (c == '{') {
            return objetoLido();
        }
        if (c == '[') {
            return listaLida();
        }
        if (c == '"') {
            return textoLido();
        }
        if (texto.startsWith("true", posicao)) {
            posicao += 4;
            return true;
        }
        if (texto.startsWith("false", posicao)) {
            posicao += 5;
            return false;
        }
        if (texto.startsWith("null", posicao)) {
            posicao += 4;
            return null;
        }
        int inicio = posicao;
        while (posicao < texto.length() && "-0123456789".indexOf(texto.charAt(posicao)) >= 0) {
            posicao++;
        }
        if (inicio == posicao) {
            throw new IllegalArgumentException("Valor inesperado na posição " + posicao);
        }
        return Long.parseLong(texto.substring(inicio, posicao));
    }

    private Map<String, Object> objetoLido() {
        Map<String, Object> saida = new LinkedHashMap<>();
        posicao++;
        espacos();
        if (texto.charAt(posicao) == '}') {
            posicao++;
            return saida;
        }
        while (true) {
            espacos();
            String chave = textoLido();
            espacos();
            esperar(':');
            saida.put(chave, valor());
            espacos();
            if (texto.charAt(posicao) == '}') {
                posicao++;
                return saida;
            }
            esperar(',');
        }
    }

    private List<Object> listaLida() {
        List<Object> saida = new ArrayList<>();
        posicao++;
        espacos();
        if (texto.charAt(posicao) == ']') {
            posicao++;
            return saida;
        }
        while (true) {
            saida.add(valor());
            espacos();
            if (texto.charAt(posicao) == ']') {
                posicao++;
                return saida;
            }
            esperar(',');
        }
    }

    private String textoLido() {
        esperar('"');
        StringBuilder saida = new StringBuilder();
        while (true) {
            char c = texto.charAt(posicao++);
            if (c == '"') {
                return saida.toString();
            }
            if (c < 0x20) {
                throw new IllegalArgumentException("Caractere de controle sem escape na posição " + posicao);
            }
            if (c != '\\') {
                saida.append(c);
                continue;
            }
            char escape = texto.charAt(posicao++);
            switch (escape) {
                case '"', '\\', '/' -> saida.append(escape);
                case 'n' -> saida.append('\n');
                case 'r' -> saida.append('\r');
                case 't' -> saida.append('\t');
                case 'b' -> saida.append('\b');
                case 'f' -> saida.append('\f');
                case 'u' -> {
                    saida.append((char) Integer.parseInt(texto.substring(posicao, posicao + 4), 16));
                    posicao += 4;
                }
                default -> throw new IllegalArgumentException("Escape inválido na posição " + posicao);
            }
        }
    }

    private void esperar(char c) {
        if (texto.charAt(posicao) != c) {
            throw new IllegalArgumentException("Esperava '" + c + "' na posição " + posicao);
        }
        posicao++;
    }

    private void espacos() {
        while (posicao < texto.length() && Character.isWhitespace(texto.charAt(posicao))) {
            posicao++;
        }
    }
}

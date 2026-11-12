package br.com.inpetu.hub.telas;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lê as regras dos CSS e calcula o contraste WCAG, para os testes de interface (portado do sistema antigo). */
final class Css {

    static final Path PASTA = Path.of("src/main/webapp/css");

    private Css() {
    }

    static String arquivo(String nome) throws IOException {
        return Files.readString(PASTA.resolve(nome));
    }

    /** Primeira regra com exatamente esse seletor, no começo da linha. */
    static String regra(String css, String seletor) {
        Matcher m = Pattern.compile("(?m)^" + Pattern.quote(seletor) + " \\{([^}]*)\\}").matcher(css);
        assertTrue(m.find(), "regra não encontrada: " + seletor);
        return m.group(1);
    }

    static String propriedade(String css, String seletor, String nome) {
        Matcher m = Pattern.compile("(?m)^\\s*" + Pattern.quote(nome) + ":\\s*([^;]+);").matcher(regra(css, seletor));
        assertTrue(m.find(), nome + " ausente em " + seletor);
        return m.group(1).trim();
    }

    static String variavel(String tokens, String nome) {
        return propriedade(tokens, ":root", nome);
    }

    /** Resolve var(--x), inclusive tokens que apontam para outros tokens. */
    static String cor(String valor, String tokens) {
        Matcher v = Pattern.compile("var\\((--[\\w-]+)\\)").matcher(valor);
        return v.matches() ? cor(variavel(tokens, v.group(1)), tokens) : valor;
    }

    static void assertContraste(String onde, String texto, String fundo, double minimo) {
        double razao = contraste(texto, fundo);
        assertTrue(razao >= minimo, onde + ": " + texto + " sobre " + fundo + " = " + String.format("%.2f", razao)
                + ":1 (mínimo " + minimo + ")");
    }

    static double contraste(String a, String b) {
        double la = luminancia(a);
        double lb = luminancia(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminancia(String hex) {
        String h = hex.replace("#", "");
        if (h.length() == 3) {
            h = h.replaceAll("(.)", "$1$1");
        }
        double[] c = new double[3];
        for (int i = 0; i < 3; i++) {
            double v = Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16) / 255.0;
            c[i] = v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
    }
}

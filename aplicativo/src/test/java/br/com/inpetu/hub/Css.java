package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Leitura das regras dos CSS do aplicativo e cálculo de contraste WCAG para os testes de interface. */
final class Css {
  private static final Path PASTA = Path.of("src/main/resources/static/css");

  private Css() {}

  static String arquivo(String nome) throws java.io.IOException {
    return Files.readString(PASTA.resolve(nome));
  }

  // Primeira regra com exatamente esse seletor, fora de @media (as regras gerais vêm antes no arquivo).
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

  static boolean temPropriedade(String css, String seletor, String nome) {
    return Pattern.compile("(?m)^\\s*" + Pattern.quote(nome) + ":").matcher(regra(css, seletor)).find();
  }

  static String variavel(String css, String nome) {
    return propriedade(css, ":root", nome);
  }

  static String cor(String valor, String css) {
    Matcher v = Pattern.compile("var\\((--[\\w-]+)\\)").matcher(valor);
    return v.matches() ? variavel(css, v.group(1)) : valor;
  }

  static int px(String valor) {
    return Integer.parseInt(valor.replace("px", ""));
  }

  static void assertContraste(String onde, String texto, String fundo, double minimo) {
    double razao = contraste(texto, fundo);
    assertTrue(
        razao >= minimo,
        onde + ": " + texto + " sobre " + fundo + " = " + String.format("%.2f", razao) + ":1 (mínimo " + minimo + ")");
  }

  static double contraste(String a, String b) {
    double la = luminancia(a), lb = luminancia(b);
    return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
  }

  private static double luminancia(String hex) {
    String h = hex.replace("#", "");
    if (h.length() == 3) h = h.replaceAll("(.)", "$1$1");
    double[] c = new double[3];
    for (int i = 0; i < 3; i++) {
      double v = Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16) / 255.0;
      c[i] = v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }
    return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
  }
}

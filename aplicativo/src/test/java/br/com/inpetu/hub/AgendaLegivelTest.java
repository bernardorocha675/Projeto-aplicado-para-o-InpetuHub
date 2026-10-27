package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Contraste WCAG AA (4,5:1 para texto) da agenda e do cabeçalho, setas de 44 px e link para pular o menu. */
class AgendaLegivelTest {
  private static final Path CSS = Path.of("src/main/resources/static/css");
  private static final Path TEMPLATES = Path.of("src/main/resources/templates");

  @Test
  void textosDaAgendaEDoCabecalhoTemContrasteAA() throws Exception {
    String style = Files.readString(CSS.resolve("style.css"));
    String site = Files.readString(CSS.resolve("site.css"));
    String azul = variavel(style, "--blue");
    String cinza = variavel(style, "--muted");
    String faixa = cor(propriedade(style, ".agenda-intro", "background"), style);
    String calendario = propriedade(style, ".agenda-shell", "background");
    assertContraste("menu sobre o cabeçalho", variavel(site, "--site-blue"), propriedade(site, ".topo", "background"));
    assertContraste("texto branco da faixa", "#ffffff", faixa);
    assertContraste("dias da semana", cor(propriedade(style, ".weekdays", "color"), style), calendario);
    assertContraste("rótulos e dica do calendário", cinza, calendario);
    assertContraste("texto cinza no detalhe do dia", cinza, "#ffffff");
    assertContraste("botão secundário", azul, "#ffffff");
    assertContraste("botão principal", "#ffffff", azul);
    assertFalse(propriedadeOpcional(style, ".agenda-intro .intro-note", "opacity"), "a transparência da nota reduz o contraste");
  }

  @Test
  void setasDoMesTemAlvoDe44px() throws Exception {
    String style = Files.readString(CSS.resolve("style.css"));
    assertTrue(px(propriedade(style, ".calendar-arrow", "width")) >= 44);
    assertTrue(px(propriedade(style, ".calendar-arrow", "height")) >= 44);
  }

  @Test
  void tituloDaAgendaSemQuebrasForcadas() throws Exception {
    String agenda = Files.readString(TEMPLATES.resolve("public/agenda.html"));
    Matcher h1 = Pattern.compile("(?s)<h1[^>]*>(.*?)</h1>").matcher(agenda);
    assertTrue(h1.find());
    assertFalse(h1.group(1).contains("<br"), "o site oficial quebra o título pela largura");
  }

  @Test
  void calendarioTemPapelParaONomeAcessivel() throws Exception {
    String agenda = Html.compacto(Files.readString(TEMPLATES.resolve("public/agenda.html")));
    // aria-label num div sem papel é proibido (axe: aria-prohibited-attr).
    assertTrue(agenda.contains("role=\"group\" aria-label=\"Calendário de disponibilidade\""));
  }

  @Test
  void todaTelaTemOConteudoQueOLinkDePularMenuAlcanca() throws Exception {
    String fragmentos = Html.compacto(Files.readString(TEMPLATES.resolve("fragments.html")));
    assertTrue(fragmentos.contains("<header th:fragment=\"topo\" class=\"topo\"><a class=\"pular\" href=\"#conteudo\">Pular para o conteúdo</a>"));
    try (Stream<Path> arquivos = Files.walk(TEMPLATES)) {
      var semAlvo =
          arquivos
              .filter(p -> p.toString().endsWith(".html"))
              .filter(p -> ler(p).contains("<main") && !ler(p).contains("<main id=\"conteudo\""))
              .toList();
      assertTrue(semAlvo.isEmpty(), "telas sem <main id=\"conteudo\">: " + semAlvo);
    }
  }

  private static String ler(Path p) {
    try {
      return Files.readString(p);
    } catch (java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  // Primeira regra com exatamente esse seletor, fora de @media (as regras gerais vêm antes no arquivo).
  private static String regra(String css, String seletor) {
    Matcher m = Pattern.compile("(?m)^" + Pattern.quote(seletor) + " \\{([^}]*)\\}").matcher(css);
    assertTrue(m.find(), "regra não encontrada: " + seletor);
    return m.group(1);
  }

  private static String propriedade(String css, String seletor, String nome) {
    Matcher m = Pattern.compile("(?m)^\\s*" + Pattern.quote(nome) + ":\\s*([^;]+);").matcher(regra(css, seletor));
    assertTrue(m.find(), nome + " ausente em " + seletor);
    return m.group(1).trim();
  }

  private static boolean propriedadeOpcional(String css, String seletor, String nome) {
    return Pattern.compile("(?m)^\\s*" + Pattern.quote(nome) + ":").matcher(regra(css, seletor)).find();
  }

  private static String variavel(String css, String nome) {
    return propriedade(css, ":root", nome);
  }

  private static String cor(String valor, String css) {
    Matcher v = Pattern.compile("var\\((--[\\w-]+)\\)").matcher(valor);
    return v.matches() ? variavel(css, v.group(1)) : valor;
  }

  private static int px(String valor) {
    return Integer.parseInt(valor.replace("px", ""));
  }

  private static void assertContraste(String onde, String texto, String fundo) {
    double razao = contraste(texto, fundo);
    assertTrue(razao >= 4.5, onde + ": " + texto + " sobre " + fundo + " = " + String.format("%.2f", razao) + ":1 (mínimo 4,5)");
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

package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Área Interna com a paleta única do site oficial, componentes padronizados e contraste WCAG AA. */
class AreaInternaTest {
  private static final Path TEMPLATES = Path.of("src/main/resources/templates");
  // Branco e sombras com transparência (8 dígitos) podem ficar escritos direto; o resto vem dos tokens.
  private static final Pattern COR_SOLTA = Pattern.compile("#(?![fF]{3}\\b|[fF]{6}\\b)(?:[0-9a-fA-F]{6}|[0-9a-fA-F]{3})\\b");

  @Test
  void umSoRootComOsTokens() throws Exception {
    long roots = 0;
    for (String f : new String[] {"style.css", "admin.css", "site.css"})
      roots += Pattern.compile("(?m)^:root \\{").matcher(Css.arquivo(f)).results().count();
    assertEquals(1L, roots, "os tokens ficam num só :root (em site.css)");
    for (String f : new String[] {"style.css", "admin.css"}) {
      Matcher m = COR_SOLTA.matcher(Css.arquivo(f));
      var soltas = new java.util.ArrayList<String>();
      while (m.find()) soltas.add(m.group());
      assertTrue(soltas.isEmpty(), f + " com cores fora dos tokens: " + soltas);
    }
    for (String f : new String[] {"style.css", "admin.css", "site.css"})
      for (String antiga : new String[] {"#087fc5", "#74818e", "#213345", "#cfdbe6", "#ffb14d", "#61a4df", "#828c95", "#596d80", "#687e90"})
        assertFalse(Css.arquivo(f).toLowerCase().contains(antiga), f + " ainda usa " + antiga);
  }

  @Test
  void contrasteDaAreaInterna() throws Exception {
    String site = Css.arquivo("site.css");
    String admin = Css.arquivo("admin.css");
    String style = Css.arquivo("style.css");
    String foco = Css.cor(Css.propriedade(style, ":is(a, button, input, select, textarea, summary):focus-visible", "outline-color"), site);
    // Indicador de foco: 3:1 (WCAG 2.4.11 e 1.4.11) contra os fundos em que aparece, inclusive a faixa azul.
    for (String fundo : new String[] {"#ffffff", Css.variavel(site, "--bg"), Css.cor(Css.variavel(site, "--blue"), site)})
      Css.assertContraste("contorno de foco", foco, fundo, 3);
    // No rodapé escuro (#222 e #171717) o contorno escuro sumiria: lá ele é branco.
    String focoRodape = Css.cor(Css.propriedade(site, ".site-footer :focus-visible", "outline-color"), site);
    Css.assertContraste("contorno de foco no rodapé", focoRodape, "#222222", 3);
    Css.assertContraste("contorno de foco no rodapé", focoRodape, "#171717", 3);
    Css.assertContraste(
        "item atual do menu interno",
        Css.cor(Css.propriedade(admin, ".sidebar nav :is(a.current, a:hover)", "color"), site),
        Css.cor(Css.propriedade(admin, ".sidebar nav :is(a.current, a:hover)", "background"), site),
        4.5);
    Css.assertContraste("menu interno", Css.cor(Css.propriedade(admin, ".sidebar nav a", "color"), site), "#ffffff", 4.5);
    Css.assertContraste("sobretítulo", Css.cor(Css.propriedade(style, ".blue", "color"), site), Css.variavel(site, "--bg"), 4.5);
    Css.assertContraste(
        "cabeçalho das tabelas",
        Css.cor(Css.propriedade(admin, "th", "color"), site),
        Css.cor(Css.propriedade(admin, "th", "background"), site),
        4.5);
    for (String estado : new String[] {"PENDENTE", "APROVADA", "RECUSADA", "CANCELADA", "CONCLUIDA"})
      Css.assertContraste(
          "situação " + estado,
          Css.cor(Css.propriedade(admin, ".badge." + estado, "color"), site),
          Css.cor(Css.propriedade(admin, ".badge." + estado, "background"), site),
          4.5);
    for (String aviso : new String[] {"error", "success", "warning"})
      Css.assertContraste(
          "aviso " + aviso,
          Css.cor(Css.propriedade(style, "." + aviso, "color"), site),
          Css.cor(Css.propriedade(style, "." + aviso, "background"), site),
          4.5);
    // Contorno dos campos: 3:1, como no formulário público.
    Css.assertContraste("contorno dos campos", Css.cor(Css.propriedade(style, "input, select, textarea", "border-color"), site), "#ffffff", 3);
  }

  @Test
  void componentesComAsMedidasDoSiteOficial() throws Exception {
    String style = Css.arquivo("style.css");
    assertEquals("8px", Css.propriedade(style, "button, .button", "border-radius"));
    assertEquals("12px", Css.propriedade(style, "input, select, textarea", "border-radius"));
    assertEquals("12px", Css.propriedade(style, ".card", "border-radius"));
    assertEquals("24px", Css.propriedade(style, ".card", "padding"));
    assertEquals("48px", Css.propriedade(style, ":where(.admin-main, .login-card) :where(input:not([type=\"checkbox\"]), select)", "min-height"));
    assertEquals("46px", Css.propriedade(style, ".admin-main :where(button, .button):not(.link, .small)", "min-height"));
    assertEquals("24px", Css.propriedade(style, "input[type=\"checkbox\"]", "width"));
    assertEquals("24px", Css.propriedade(style, "input[type=\"checkbox\"]", "height"));
    assertTrue(Css.propriedade(style, ".alert", "border-left").startsWith("4px solid"), "aviso com a borda esquerda da cor do estado");
    String celular = Css.media(Css.arquivo("admin.css"), "@media (max-width: 760px)");
    assertEquals("44px", Css.propriedade(celular, ".admin-main :where(button, .button, summary, .check)", "min-height"));
  }

  @Test
  void linksSoltosDaAreaInternaTemAlvoDeToque() throws Exception {
    // Links fora de frases e de tabelas: a classe link-toque dá 44 px no celular (as tabelas ficam com o lote 5).
    String[][] casos = {
      {"admin/crud/lista.html", ">Limpar</a>"},
      {"admin/reservas/lista.html", ">Limpar</a>"},
      {"admin/dashboard.html", ">Analisar solicitações →</a>"},
      {"admin/dashboard.html", ">Consultar CRM →</a>"},
      {"admin/config.html", ">Consultar auditoria</a>"},
    };
    for (String[] c : casos) {
      String html = Html.compacto(Files.readString(TEMPLATES.resolve(c[0])));
      Matcher a = Pattern.compile("<a [^>]*" + Pattern.quote(c[1])).matcher(html);
      assertTrue(a.find() && a.group().contains("class=\"link-toque\""), c[0] + ": " + c[1]);
    }
  }

  @Test
  void avisosSaoAnunciados() throws Exception {
    try (Stream<Path> arquivos = Files.walk(TEMPLATES)) {
      for (Path p : arquivos.filter(x -> x.toString().endsWith(".html")).toList()) {
        String html = Html.compacto(Files.readString(p));
        assertFalse(html.contains("<ul class=\"alert"), p + ": aviso em lista sem itens anunciados");
        Matcher m = Pattern.compile("<(p|div)[^>]*class=\"alert ([a-z]+)[^\"]*\"[^>]*>").matcher(html);
        while (m.find()) {
          String esperado = m.group(2).equals("error") ? "role=\"alert\"" : "role=\"status\"";
          assertTrue(m.group().contains(esperado), p + ": " + m.group() + " sem " + esperado);
        }
      }
    }
  }
}

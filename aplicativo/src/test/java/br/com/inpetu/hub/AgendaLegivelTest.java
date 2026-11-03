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
  private static final Path TEMPLATES = Path.of("src/main/resources/templates");

  @Test
  void textosDaAgendaEDoCabecalhoTemContrasteAA() throws Exception {
    String style = Css.arquivo("style.css");
    String site = Css.arquivo("site.css");
    String azul = Css.cor(Css.variavel(site, "--blue"), site);
    String cinza = Css.variavel(site, "--muted");
    String faixa = Css.cor(Css.propriedade(style, ".agenda-intro", "background"), site);
    String calendario = Css.cor(Css.propriedade(style, ".agenda-shell", "background"), site);
    assertContraste("menu sobre o cabeçalho", Css.variavel(site, "--site-blue"), Css.propriedade(site, ".topo", "background"));
    assertContraste("texto branco da faixa", "#ffffff", faixa);
    assertContraste("dias da semana", Css.cor(Css.propriedade(style, ".weekdays", "color"), site), calendario);
    assertContraste("rótulos e dica do calendário", cinza, calendario);
    assertContraste("texto cinza no detalhe do dia", cinza, "#ffffff");
    assertContraste("botão secundário", azul, "#ffffff");
    assertContraste("botão principal", "#ffffff", azul);
    assertFalse(Css.temPropriedade(style, ".agenda-intro .intro-note", "opacity"), "a transparência da nota reduz o contraste");
  }

  @Test
  void setasDoMesTemAlvoDe44px() throws Exception {
    String style = Css.arquivo("style.css");
    assertTrue(Css.px(Css.propriedade(style, ".calendar-arrow", "width")) >= 44);
    assertTrue(Css.px(Css.propriedade(style, ".calendar-arrow", "height")) >= 44);
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

  private static void assertContraste(String onde, String texto, String fundo) {
    Css.assertContraste(onde, texto, fundo, 4.5);
  }
}

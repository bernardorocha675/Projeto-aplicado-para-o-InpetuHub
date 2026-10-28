package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Menu do cabeçalho com botão no celular, submenus que abrem por botão e alvos de toque de 44 px. */
class MenuCelularTest {
  private static final Path TEMPLATES = Path.of("src/main/resources/templates");

  @Test
  void cabecalhoTemBotaoDoMenuLigadoAoMenu() throws Exception {
    String html = Html.compacto(Files.readString(TEMPLATES.resolve("fragments.html")));
    // Escondido sem JavaScript: o menu continua aberto como antes.
    assertTrue(
        html.contains(
            "<button type=\"button\" class=\"menu-botao\" aria-expanded=\"false\" aria-controls=\"menu-principal\" aria-label=\"Menu\" hidden>"
                + "<span class=\"menu-icone\" aria-hidden=\"true\"></span></button><nav id=\"menu-principal\" aria-label=\"Menu principal\">"),
        "botão do menu ausente ou sem aria-controls");
    assertTrue(html.contains("<script th:src=\"@{/js/menu.js}\" defer></script>"), "menu.js não é carregado em todas as telas");
  }

  @Test
  void cadaSubmenuTemBotaoQueOControla() throws Exception {
    String html = Html.compacto(Files.readString(TEMPLATES.resolve("fragments.html")));
    Matcher listas = Pattern.compile("<ul class=\"submenu\" id=\"([\\w-]+)\">").matcher(html);
    int n = 0;
    while (listas.find()) {
      n++;
      assertTrue(
          Pattern.compile(
                  "<button type=\"button\" class=\"submenu-botao\" aria-expanded=\"false\" aria-controls=\""
                      + listas.group(1)
                      + "\" aria-label=\"Abrir submenu de [^\"]+\" hidden></button><ul class=\"submenu\" id=\""
                      + listas.group(1)
                      + "\">")
              .matcher(html)
              .find(),
          "submenu sem botão: " + listas.group(1));
    }
    assertEquals(2, n, "Iniciativas e Notícias e Eventos");
  }

  @Test
  void alvosDeToqueDoCabecalhoRodapeEAgendaTem44px() throws Exception {
    String site = Css.arquivo("site.css");
    String celular = Css.media(site, "@media (max-width: 980px)");
    assertEquals("44px", Css.propriedade(celular, ".menu-botao:not([hidden])", "width"));
    assertEquals("44px", Css.propriedade(celular, ".menu-botao:not([hidden])", "height"));
    assertEquals("44px", Css.propriedade(celular, ".topo nav a", "min-height"));
    assertEquals("44px", Css.propriedade(celular, ".topo .logo", "min-height"));
    assertEquals("none", Css.propriedade(celular, ".topo[data-menu=\"fechado\"] nav", "display"));
    assertEquals("44px", Css.propriedade(celular, ".site-footer .footer-bottom a", "min-width"));
    assertEquals("44px", Css.propriedade(celular, ".site-footer .footer-bottom a", "min-height"));
    String style = Css.arquivo("style.css");
    assertEquals("44px", Css.propriedade(Css.media(style, "@media (max-width: 760px)"), ".calendar-grid .day", "height"));
    assertEquals("44px", Css.propriedade(celular, ".calendar-filter :is(select, input)", "min-height"));
    assertEquals("44px", Css.propriedade(celular, ".link-toque", "min-height"));
    String admin = Css.media(Css.arquivo("admin.css"), "@media (max-width: 760px)");
    assertEquals("44px", Css.propriedade(admin, ".sidebar nav a", "min-height"));
    assertEquals("44px", Css.propriedade(admin, ".sidebar form button", "min-height"));
  }

  @Test
  void tituloDaMissaoNoInicioEhLegivel() throws Exception {
    // O ciano oficial #00c3ff tem 2,04:1 sobre branco; título grande pede ao menos 3:1, e o azul do site passa com folga.
    String site = Css.arquivo("site.css");
    Css.assertContraste("Nossa missão", Css.cor(Css.propriedade(site, ".mission > h2", "color"), site), "#ffffff", 4.5);
  }

  @Test
  void submenuNaoAbrePeloFocoQuandoHaBotao() throws Exception {
    String site = Css.arquivo("site.css");
    // Com o script, o Tab não atravessa os itens do submenu; só sem JavaScript o foco ainda abre a lista.
    assertTrue(site.contains(".topo:not([data-menu]) .menu-group:focus-within .submenu"));
    assertFalse(site.contains(".topo .menu-group:focus-within .submenu"));
  }
}

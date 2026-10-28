package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Pedido de reserva no padrão do formulário de contato oficial, com campos que passam no WCAG AA. */
class PedidoReservaOficialTest {
  private static final String CAMPOS = ".form-oficial :where(input:not([type=\"checkbox\"]), select, textarea)";
  private static final Path TEMPLATES = Path.of("src/main/resources/templates/public");

  @Test
  void camposTemAsMedidasDoFormularioOficialEContornoVisivel() throws Exception {
    String site = Css.arquivo("site.css");
    assertEquals("65px", Css.propriedade(site, CAMPOS, "min-height"));
    assertEquals("12px", Css.propriedade(site, CAMPOS, "border-radius"));
    // :where não soma especificidade, então a altura própria da área de texto (200 px, como no oficial) prevalece.
    assertEquals("200px", Css.propriedade(site, ".form-oficial textarea", "min-height"));
    assertEquals("var(--site-fundo-campo)", Css.propriedade(site, CAMPOS, "background"));
    assertEquals("1px solid var(--site-borda-campo)", Css.propriedade(site, CAMPOS, "border"));
    // O contorno do campo precisa de 3:1 (WCAG 1.4.11); o #e2e8f0 do site oficial tem 1,2:1.
    Css.assertContraste("contorno do campo", Css.variavel(site, "--site-borda-campo"), Css.variavel(site, "--site-fundo-campo"), 3);
    Css.assertContraste("texto do campo", Css.cor(Css.propriedade(site, CAMPOS, "color"), site), Css.variavel(site, "--site-fundo-campo"), 4.5);
    Css.assertContraste("mensagem de erro", Css.variavel(site, "--site-erro"), "#ffffff", 4.5);
  }

  @Test
  void botaoGrandeECaixasDeMarcarTemAlvoGrande() throws Exception {
    String site = Css.arquivo("site.css");
    assertEquals("64px", Css.propriedade(site, ".botao-grande", "min-height"));
    assertEquals("var(--site-blue)", Css.propriedade(site, ".botao-grande", "background"));
    Css.assertContraste("botão grande", "#ffffff", Css.variavel(site, "--site-blue"), 4.5);
    assertTrue(Css.px(Css.propriedade(site, ".form-oficial .checks input[type=\"checkbox\"]", "width")) >= 24);
    assertTrue(Css.px(Css.propriedade(site, ".form-oficial .checks label", "min-height")) >= 44);
  }

  @Test
  void telasDoPedidoUsamAFaixaEOCartaoOficiais() throws Exception {
    for (String tela : new String[] {"reserva-form.html", "reserva-resumo.html", "reserva-sucesso.html", "confirmar.html"}) {
      String html = Files.readString(TEMPLATES.resolve(tela));
      assertTrue(html.contains("class=\"faixa-pagina\""), tela + " sem a faixa da página");
      assertTrue(html.contains("cartao-oficial"), tela + " sem o cartão oficial");
      assertFalse(html.contains("eyebrow blue"), tela + " ainda com o sobretítulo antigo");
    }
    assertTrue(Files.readString(TEMPLATES.resolve("reserva-form.html")).contains("class=\"card cartao-oficial form-oficial\""));
  }

  @Test
  void confirmacaoVoltaParaAAgendaESugereOCodigoRecebido() throws Exception {
    String html = Html.compacto(Files.readString(TEMPLATES.resolve("confirmar.html")));
    assertTrue(html.contains("autocomplete=\"one-time-code\""));
    assertTrue(html.contains("<a href=\"/agenda\" class=\"link-toque\">Voltar à agenda</a>"), "o link dizia agenda, mas levava ao início");
  }
}

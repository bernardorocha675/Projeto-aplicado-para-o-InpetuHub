package br.com.inpetu.hub.telas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/** Porta do PedidoReservaOficialTest: medidas do formulário de contato oficial, com contraste WCAG AA. */
class PedidoLegivelTest {

    private static final String CAMPOS = ".form-oficial :where(input:not([type=\"checkbox\"]), select, textarea)";
    private static final Path TELAS = Path.of("src/main/webapp/WEB-INF/jsp/solicitacao");

    @Test
    void camposTemAsMedidasDoFormularioOficialEContornoVisivel() throws Exception {
        String tokens = Css.arquivo("tokens.css");
        String pedido = Css.arquivo("pedido.css");
        assertEquals("65px", Css.propriedade(pedido, CAMPOS, "min-height"));
        assertEquals("12px", Css.propriedade(pedido, CAMPOS, "border-radius"));
        assertEquals("200px", Css.propriedade(pedido, ".form-oficial textarea", "min-height"));
        assertEquals("1px solid var(--site-borda-campo)", Css.propriedade(pedido, CAMPOS, "border"));
        String fundo = Css.cor(Css.propriedade(pedido, CAMPOS, "background"), tokens);
        Css.assertContraste("contorno do campo", Css.cor("var(--site-borda-campo)", tokens), fundo, 3);
        Css.assertContraste("texto do campo", Css.cor(Css.propriedade(pedido, CAMPOS, "color"), tokens), fundo, 4.5);
        Css.assertContraste("mensagem de erro", Css.cor("var(--erro)", tokens), "#ffffff", 4.5);
    }

    @Test
    void botaoGrandeECaixasDeMarcarTemAlvoGrande() throws Exception {
        String tokens = Css.arquivo("tokens.css");
        String pedido = Css.arquivo("pedido.css");
        assertEquals("64px", Css.propriedade(pedido, ".botao-grande", "min-height"));
        Css.assertContraste("botão grande", "#ffffff", Css.cor(Css.propriedade(pedido, ".botao-grande", "background"),
                tokens), 4.5);
        assertEquals("24px", Css.propriedade(pedido, ".form-oficial .caixas input", "width"));
        assertEquals("44px", Css.propriedade(pedido, ".form-oficial .caixas label", "min-height"));
    }

    @Test
    void telasDoPedidoUsamAFaixaEOCartaoOficiais() throws Exception {
        for (String tela : new String[]{"formulario.jsp", "resumo.jsp", "recebida.jsp"}) {
            String jsp = Files.readString(TELAS.resolve(tela));
            assertTrue(jsp.contains("class=\"faixa-pagina\""), tela + " sem a faixa da página");
            assertTrue(jsp.contains("cartao-oficial"), tela + " sem o cartão oficial");
            assertTrue(jsp.contains("estilo=\"/css/pedido.css\""), tela);
        }
    }
}

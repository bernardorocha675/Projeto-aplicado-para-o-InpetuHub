package br.com.inpetu.hub.telas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tabela larga rola dentro do seu contêiner, sem alargar a página no celular. */
class TabelasTest {

    @Test
    void textoParaLeitorDeTelaNaoEscapaDaRolagemDaTabela() throws Exception {
        String base = Css.arquivo("base.css");
        assertEquals("auto", Css.propriedade(base, ".rolagem-tabela", "overflow-x"));
        // Sem isto, o .so-leitor (position: absolute) dentro da tabela alargava a página a 575 px em 390 px.
        assertEquals("relative", Css.propriedade(base, ".rolagem-tabela", "position"));
        assertTrue(Css.regra(base, ".so-leitor").contains("position: absolute"));
    }
}

package br.com.inpetu.hub.telas;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/** Revisão da entrega 12: ações que não têm volta pedem confirmação, e os textos seguem a tipografia. */
class AcoesDestrutivasTest {

    private static final Path WEB = Path.of("src/main/webapp");

    private static String ler(String caminho) throws Exception {
        return Files.readString(WEB.resolve(caminho));
    }

    @Test
    void excluirRecusarECancelarPedemConfirmacao() throws Exception {
        String[][] formularios = {{"WEB-INF/jsp/espaco/lista.jsp", "/admin/cadastros/espacos/excluir"},
                {"WEB-INF/jsp/interna/solicitacoes.jsp", "/admin/solicitacoes/recusar"},
                {"WEB-INF/jsp/interna/reserva.jsp", "/cancelar\""}};
        for (String[] f : formularios) {
            String jsp = ler(f[0]);
            int acao = jsp.indexOf(f[1]);
            assertTrue(acao > 0, f[0]);
            String abertura = jsp.substring(jsp.lastIndexOf("<form", acao), jsp.indexOf('>', acao));
            assertTrue(abertura.contains("data-confirmar=\""), f[0] + " sem confirmação: " + abertura);
        }
        assertTrue(ler("WEB-INF/tags/interna.tag").contains("/js/confirmar.js"), "o script vem com o layout");
        String js = ler("js/confirmar.js");
        assertTrue(js.contains("data-confirmar") && js.contains("preventDefault"), "cancela o envio sem o ok");
    }

    @Test
    void textosDeEsperaEExemploTerminamComReticencias() throws Exception {
        assertFalse(ler("js/agenda.js").contains("..."), "use … em vez de ...");
        assertTrue(ler("WEB-INF/jsp/interna/reservas.jsp").contains("placeholder=\"Nome, empresa ou documento…\""));
    }
}

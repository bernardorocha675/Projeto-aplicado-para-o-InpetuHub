package br.com.inpetu.hub.comum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import br.com.inpetu.hub.comum.log.CapturaDeLog;

/** Erros viram páginas amigáveis, sem detalhe técnico, e o log guarda a causa sem as mensagens. */
@Timeout(60)
class ErrosTest {

    private static Servidor servidor;

    @BeforeAll
    static void subir() throws Exception {
        servidor = Servidor.iniciar(0, RotasDeTeste.todas());
    }

    @AfterAll
    static void descer() {
        servidor.close();
    }

    @Test
    void falhaInesperadaDaPaginaAmigavelEOLogTemACausaSemAMensagem() throws Exception {
        try (CapturaDeLog log = new CapturaDeLog()) {
            HttpResponse<String> resposta = new Http(servidor).get("/teste/falha");

            assertEquals(500, resposta.statusCode());
            String id = resposta.headers().firstValue("X-Request-ID").orElse(null);
            assertNotNull(id);
            assertTrue(resposta.body().contains("Não foi possível concluir"), resposta.body());
            assertTrue(resposta.body().contains(id), "a página mostra a referência para suporte");
            semDetalheTecnico(resposta.body());

            String texto = log.aguardar("REQUEST_FAILED");
            assertTrue(texto.contains("REQUEST_FAILED"), texto);
            assertTrue(texto.contains("java.lang.IllegalStateException"), texto);
            assertTrue(texto.contains("RotasDeTeste$Falha.doGet"), texto);
            assertTrue(texto.contains(id), texto);
            assertFalse(texto.contains("private@example.com"), texto);
        }
    }

    @Test
    void regraRecusadaE400ComAMensagemDaRegraEAvisoNoLog() throws Exception {
        try (CapturaDeLog log = new CapturaDeLog()) {
            HttpResponse<String> resposta = new Http(servidor).get("/teste/regra");

            assertEquals(400, resposta.statusCode());
            assertTrue(resposta.body().contains("Confira o horário."), resposta.body());
            semDetalheTecnico(resposta.body());
            String texto = log.aguardar("REQUEST_REJECTED");
            assertTrue(texto.contains("REQUEST_REJECTED"), texto);
            assertTrue(texto.contains("\"level\":\"WARN\""), texto);
        }
    }

    @Test
    void conflitoNoBancoE409SemSqlNemDadosPessoais() throws Exception {
        try (CapturaDeLog log = new CapturaDeLog()) {
            HttpResponse<String> resposta = new Http(servidor).get("/teste/conflito");

            assertEquals(409, resposta.statusCode());
            assertTrue(resposta.body().contains("Registro duplicado"), resposta.body());
            semDetalheTecnico(resposta.body());
            String texto = log.aguardar("SQLIntegrityConstraintViolationException");
            assertTrue(texto.contains("SQLIntegrityConstraintViolationException"), texto);
            assertFalse(texto.contains("sql-password"), texto);
        }
    }

    @Test
    void paginaInexistenteE404EMetodoErradoE405SemFalhaNoLog() throws Exception {
        try (CapturaDeLog log = new CapturaDeLog()) {
            Http http = new Http(servidor);
            HttpResponse<String> inexistente = http.get("/nao-existe?email=fulano@example.com");
            assertEquals(404, inexistente.statusCode());
            assertTrue(inexistente.body().contains("Página não encontrada"), inexistente.body());
            semDetalheTecnico(inexistente.body());

            String token = http.get("/teste/formulario").body();
            HttpResponse<String> metodo = http.post("/saude", "_csrf=" + token);
            assertEquals(405, metodo.statusCode());
            semDetalheTecnico(metodo.body());

            String texto = log.aguardar("status=405");
            assertTrue(texto.contains("status=404"), texto);
            assertFalse(texto.contains("REQUEST_FAILED"), texto);
            assertFalse(texto.contains("fulano"), "o endereço pedido não vai para o log: " + texto);
            assertFalse(texto.contains("nao-existe"), "caminho livre vira OTHER: " + texto);
        }
    }

    @Test
    void paginasDeErroUsamOLayoutComPularParaOConteudo() throws Exception {
        String corpo = new Http(servidor).get("/nao-existe").body();
        assertTrue(corpo.startsWith("<!doctype html>"), corpo);
        assertTrue(corpo.contains("<html lang=\"pt-BR\">"), corpo);
        assertTrue(corpo.contains("<title>Página não encontrada | InPETU Hub</title>"), corpo);
        int pular = corpo.indexOf("<a class=\"pular\" href=\"#conteudo\">Pular para o conteúdo</a>");
        assertTrue(pular > 0, corpo);
        assertEquals(pular, corpo.indexOf("<a "), "o link de pular é o primeiro link da página");
        assertTrue(corpo.contains("<main id=\"conteudo\""), corpo);
        assertTrue(corpo.contains("/css/tokens.css") && corpo.contains("/css/base.css"), corpo);
    }

    private static void semDetalheTecnico(String corpo) {
        for (String proibido : new String[]{"Jetty", "Exception", "sql-password", "at br.", "jakarta.", "STATUS:"}) {
            assertFalse(corpo.contains(proibido), "a página não mostra '" + proibido + "': " + corpo);
        }
    }
}

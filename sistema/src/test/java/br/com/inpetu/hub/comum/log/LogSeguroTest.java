package br.com.inpetu.hub.comum.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.AccessDeniedException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Log em JSON por linha: só campos controlados, nunca mensagens livres nem dados pessoais. */
class LogSeguroTest {

    private final LogSeguro formato = new LogSeguro();

    @AfterEach
    void limpar() {
        System.clearProperty("inpetu.app.version");
        System.clearProperty("inpetu.launcher.version");
        System.clearProperty("inpetu.session.id");
        ContextoRequisicao.limpar();
    }

    @Test
    void cadaLinhaTemAsVersoesESoUmIdentificadorDeSessaoValido() {
        System.setProperty("inpetu.app.version", "1.0.0-alpha.3");
        System.setProperty("inpetu.launcher.version", "1.0.0");
        System.setProperty("inpetu.session.id", "0123456789abcdef0123456789abcdef");
        String linha = formato.format(evento(Level.INFO, "event=SERVER_READY"));
        assertTrue(linha.endsWith("}\n"), linha);
        assertTrue(linha.contains("\"appVersion\":\"1.0.0-alpha.3\""), linha);
        assertTrue(linha.contains("\"launcherVersion\":\"1.0.0\""), linha);
        assertTrue(linha.contains("\"sessionId\":\"0123456789abcdef0123456789abcdef\""), linha);
        assertTrue(linha.contains("\"level\":\"INFO\""), linha);
        assertTrue(linha.contains("\"requestId\":\"-\""), linha);
        assertEquals(1, linha.chars().filter(c -> c == '\n').count(), "uma linha por evento");

        System.setProperty("inpetu.app.version", "1.0.0\"},\"x\":\"y");
        System.setProperty("inpetu.launcher.version", "0.5.12");
        System.setProperty("inpetu.session.id", "fulano@example.com");
        String recusada = formato.format(evento(Level.INFO, "event=SERVER_READY"));
        assertTrue(recusada.contains("\"appVersion\":\"unknown\""), recusada);
        assertTrue(recusada.contains("\"launcherVersion\":\"unknown\""), "só versões 1.x: " + recusada);
        assertTrue(recusada.contains("\"sessionId\":\"standalone\""), recusada);
        assertFalse(recusada.contains("fulano"), recusada);
    }

    @Test
    void mensagemDeBibliotecaEDeExcecaoNuncaESerializada() {
        LogRecord jetty = new LogRecord(Level.WARNING,
                "Falhou ao abrir /home/fulano/senha=123 para fulano@example.com");
        jetty.setLoggerName("org.eclipse.jetty.server.Server");
        jetty.setThrown(new IllegalStateException("cnpj 12345678000190 email fulano@example.com"));
        String linha = formato.format(jetty);
        assertTrue(linha.contains("\"message\":\"FRAMEWORK_EVENT\""), linha);
        assertTrue(linha.contains("\"level\":\"WARN\""), linha);
        assertTrue(linha.contains("\"exceptionType\":\"java.lang.IllegalStateException\""), linha);
        for (String proibido : new String[]{"fulano", "senha", "12345678000190", "/home"}) {
            assertFalse(linha.contains(proibido), proibido + ": " + linha);
        }
    }

    @Test
    void aCausaRealApareceSoComNomesDeClasseELinhas() {
        SQLException banco = new SQLException("insert into empreendedor values ('fulano@example.com')", "23505", 23505);
        RuntimeException topo = new RuntimeException("falhou para /home/fulano", new IllegalStateException(
                "chave em /home/fulano/.secrets", banco));
        String linha = formato.format(evento(Level.SEVERE, "event=SERVER_START_FAILED", topo));
        assertTrue(linha.contains("\"level\":\"ERROR\""), linha);
        assertTrue(linha.contains("\"causes\":[\"java.lang.IllegalStateException\",\"java.sql.SQLException\"]"),
                linha);
        assertTrue(linha.contains("\"rootFrames\":["), linha);
        assertTrue(linha.contains("\"failure\":\"DATABASE_ERROR\""), linha);
        assertTrue(linha.contains("\"sqlState\":\"23505\""), linha);
        assertTrue(linha.contains("\"sqlErrorCode\":23505"), linha);
        assertTrue(linha.contains("LogSeguroTest.aCausaRealApareceSoComNomesDeClasseELinhas:"), linha);
        assertFalse(linha.contains("fulano"), linha);
        assertFalse(linha.contains("insert"), linha);
    }

    @Test
    void cadeiaDeCausasELinhasTemLimite() {
        Throwable cadeia = new IllegalStateException("fim");
        for (int i = 0; i < 20; i++) {
            cadeia = new RuntimeException("nível " + i, cadeia);
        }
        String linha = formato.format(evento(Level.SEVERE, "event=REQUEST_FAILED", cadeia));
        assertEquals(FalhaCausa.MAX_CAUSAS, contar(lista(linha, "causes"), "\""), "até 8 causas: " + linha);
        assertTrue(contar(lista(linha, "frames"), "\"") <= 20, linha);
        assertTrue(contar(lista(linha, "rootFrames"), "\"") <= 10, linha);
    }

    @Test
    void falhaAoIniciarEClassificadaPelaCausa() {
        assertEquals("PORT_IN_USE", FalhaCausa.de(new RuntimeException(new java.net.BindException("x"))).categoria());
        assertEquals("DATABASE_IN_USE",
                FalhaCausa.de(new RuntimeException(new SQLException("x", "90020", 90020))).categoria());
        assertEquals("MIGRATION_FAILED",
                FalhaCausa.de(new org.flywaydb.core.api.FlywayException("x")).categoria());
        assertEquals("DATABASE_ERROR", FalhaCausa.de(new SQLException("x", "42000", 1064)).categoria());
        assertEquals("FILE_ACCESS", FalhaCausa.de(new AccessDeniedException("/home/fulano")).categoria());
        assertEquals("OUT_OF_MEMORY", FalhaCausa.de(new OutOfMemoryError()).categoria());
        assertEquals("OTHER", FalhaCausa.de(new IllegalArgumentException()).categoria());
        assertEquals(null, FalhaCausa.de(new SQLException("x", "fulano@x", -5)).sqlState());
        assertEquals(null, FalhaCausa.de(new SQLException("x", "42000", -5)).codigoSql(),
                "código negativo é descartado");
    }

    @Test
    void rotasViramCategoriasSemIdsNemConsulta() {
        assertEquals("/saude", RotasSeguras.canonica("/saude"));
        assertEquals("ASSET", RotasSeguras.canonica("/css/tokens.css"));
        assertEquals("ASSET", RotasSeguras.canonica("/img/logo.png"));
        assertEquals("OTHER", RotasSeguras.canonica("/fulano@example.com"));
        assertEquals("OTHER", RotasSeguras.canonica("/admin/reservas/123?cnpj=1"));
        assertEquals("OTHER", RotasSeguras.canonica(null));
        assertEquals("OTHER", RotasSeguras.canonica("/" + "a".repeat(300)));
    }

    @Test
    void eventosRecusamCamposLivres() throws Exception {
        try (CapturaDeLog log = new CapturaDeLog()) {
            Eventos.requisicao("PROPFIND\"x", "/admin/reservas/42?email=fulano@example.com", 404, 3, null);
            String texto = log.texto();
            assertTrue(texto.contains("event=HTTP method=OTHER route=OTHER status=404 durationMs=3"), texto);
            assertTrue(texto.contains("\"level\":\"WARN\""), texto);
            assertFalse(texto.contains("fulano"), texto);
        }
    }

    @Test
    void identificadorDaRequisicaoSoComOFormatoEsperado() {
        ContextoRequisicao.definir("0f8fad5b-d9cb-469f-a165-70867728950e");
        assertTrue(formato.format(evento(Level.INFO, "event=X")).contains(
                "\"requestId\":\"0f8fad5b-d9cb-469f-a165-70867728950e\""));
        ContextoRequisicao.definir("fulano@example.com");
        assertTrue(formato.format(evento(Level.INFO, "event=X")).contains("\"requestId\":\"-\""));
        ContextoRequisicao.limpar();
        assertTrue(formato.format(evento(Level.INFO, "event=X")).contains("\"requestId\":\"-\""));
    }

    @Test
    void caracteresDeControleSaoEscapados() {
        String linha = formato.format(evento(Level.INFO, "event=X\n\"y\"\u0001"));
        assertTrue(linha.contains("event=X\\u000a\\\"y\\\"\\u0001"), linha);
        assertEquals(1, linha.chars().filter(c -> c == '\n').count(), linha);
    }

    private static LogRecord evento(Level nivel, String mensagem) {
        return evento(nivel, mensagem, null);
    }

    private static LogRecord evento(Level nivel, String mensagem, Throwable erro) {
        LogRecord registro = new LogRecord(nivel, mensagem);
        registro.setLoggerName(Eventos.class.getName());
        registro.setThrown(erro);
        return registro;
    }

    private static String lista(String linha, String nome) {
        Matcher m = Pattern.compile("\"" + nome + "\":\\[([^\\]]*)\\]").matcher(linha);
        assertTrue(m.find(), nome + " ausente: " + linha);
        return m.group(1);
    }

    private static long contar(String texto, String aspas) {
        return texto.isEmpty() ? 0 : texto.split(aspas, -1).length / 2;
    }
}

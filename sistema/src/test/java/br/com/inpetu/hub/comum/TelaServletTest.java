package br.com.inpetu.hub.comum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import br.com.inpetu.hub.comum.log.CapturaDeLog;

/** O que acontece na página chega ao log só por códigos de uma lista, sem texto livre. */
@Timeout(60)
class TelaServletTest {

    private static final String CABECALHO = "X-Inpetu-Tela";

    private static int avisar(Http http, String consulta) throws Exception {
        return http.get("/tela?" + consulta, CABECALHO, "1").statusCode();
    }

    @Test
    void codigosDaListaVaoAoLogSemCriarSessao() throws Exception {
        try (Servidor servidor = Servidor.iniciar(0, AmbienteDeTeste.novo()); CapturaDeLog log = new CapturaDeLog()) {
            Http http = new Http(servidor);
            assertEquals(204, avisar(http, "evento=CLIQUE&alvo=DIA&pagina=/agenda"));
            assertEquals(204, avisar(http, "evento=CAMPO_INVALIDO&alvo=email,cidade&pagina=/reserva"));
            assertEquals(204, avisar(http, "evento=ERRO_SCRIPT&alvo=agenda.js&linha=51&pagina=/agenda"));
            assertEquals(204, avisar(http, "evento=CLIQUE&alvo=CONFIRMAR_NAO&pagina=/admin/reservas/987654"));
            String texto = log.aguardar("CONFIRMAR_NAO");
            assertTrue(texto.contains("event=SCREEN type=CLIQUE target=DIA page=/agenda"), texto);
            assertTrue(texto.contains("event=SCREEN type=CAMPO_INVALIDO fields=email,cidade page=/reserva"), texto);
            assertTrue(texto.contains("event=SCREEN type=ERRO_SCRIPT target=agenda.js page=/agenda line=51"), texto);
            assertTrue(texto.contains("page=/admin/reservas/{id}"), texto);
            assertFalse(texto.contains("987654"), texto);
            assertNull(http.sessao());
        }
    }

    @Test
    void foraDaListaESemOCabecalhoERecusado() throws Exception {
        try (Servidor servidor = Servidor.iniciar(0, AmbienteDeTeste.novo()); CapturaDeLog log = new CapturaDeLog()) {
            Http http = new Http(servidor);
            assertEquals(403, http.get("/tela?evento=CLIQUE&alvo=DIA&pagina=/agenda").statusCode(),
                    "outro site não consegue mandar o cabeçalho");
            for (String ruim : new String[]{"evento=CLIQUE&alvo=FulanoSigiloso", "evento=FulanoSigiloso&alvo=DIA",
                    "evento=CLIQUE&alvo=email", "evento=CLIQUE&alvo=DIA,DIA",
                    "evento=CAMPO_INVALIDO&alvo=email,Sigiloso", "evento=ERRO_SCRIPT&alvo=FulanoSigiloso.js",
                    "evento=CLIQUE", ""}) {
                assertEquals(400, avisar(http, ruim), ruim);
            }
            assertEquals(204, avisar(http, "evento=CLIQUE&alvo=DIA&pagina=/FulanoSigiloso"));
            assertEquals(204, avisar(http, "evento=ERRO_SCRIPT&alvo=agenda.js&linha=FulanoSigiloso&pagina=/agenda"));
            String texto = log.aguardar("ERRO_SCRIPT");
            assertTrue(texto.contains("target=DIA page=OTHER"), texto);
            assertFalse(texto.contains("Sigiloso"), texto);
        }
    }

    @Test
    void temLimitePorMinuto() throws Exception {
        try (Servidor servidor = Servidor.iniciar(0, AmbienteDeTeste.novo())) {
            Http http = new Http(servidor);
            int ultima = 0;
            for (int i = 0; i < 121; i++) {
                ultima = avisar(http, "evento=CLIQUE&alvo=DIA&pagina=/agenda");
            }
            assertEquals(429, ultima);
        }
    }

    @Test
    void todasAsTelasCarregamOScriptEOsCodigosDasTelasEstaoNaLista() throws Exception {
        Path web = Path.of("src/main/webapp");
        assertTrue(Files.readString(web.resolve("WEB-INF/tags/pagina.tag")).contains("src=\"/js/tela.js\""));
        String js = Files.readString(web.resolve("js/tela.js"));
        assertFalse(js.contains(".value"), "nunca lê o que foi digitado");
        assertFalse(js.contains("textContent") || js.contains("innerText") || js.contains(".message"), js);
        Matcher codigo = Pattern.compile("'([A-Z][A-Z_]+)'").matcher(js);
        while (codigo.find()) {
            assertTrue(TelaServlet.conhecido(codigo.group(1)), codigo.group(1));
        }
        try (Stream<Path> scripts = Files.list(web.resolve("js"))) {
            for (Path script : scripts.toList()) {
                assertTrue(TelaServlet.conhecido(script.getFileName().toString()), script.toString());
            }
        }
        try (Stream<Path> telas = Files.walk(web.resolve("WEB-INF"))) {
            for (Path tela : telas.filter(Files::isRegularFile).toList()) {
                Matcher campo = Pattern
                        .compile("<(?:input|select|textarea)[^>]*? name=\"([A-Za-z]+)\"|<t:campo nome=\"([A-Za-z]+)\"")
                        .matcher(Files.readString(tela));
                while (campo.find()) {
                    String nome = campo.group(1) != null ? campo.group(1) : campo.group(2);
                    assertTrue(TelaServlet.conhecido(nome), nome + " em " + tela);
                }
            }
        }
    }
}

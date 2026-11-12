package br.com.inpetu.hub.comum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Cabeçalhos iguais aos do sistema antigo, CSRF por token na sessão e recusa de hosts externos. */
@Timeout(60)
class SegurancaWebTest {

    static final String CSP = "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:;"
            + " connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'";

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
    void cabecalhosDeSegurancaEmPaginasRespostasDeErroEArquivos() throws Exception {
        Http http = new Http(servidor);
        for (String caminho : new String[]{"/saude", "/nao-existe", "/css/tokens.css"}) {
            HttpResponse<String> resposta = http.get(caminho);
            assertEquals(CSP, cabecalho(resposta, "Content-Security-Policy"), caminho);
            assertEquals("nosniff", cabecalho(resposta, "X-Content-Type-Options"), caminho);
            assertEquals("no-referrer", cabecalho(resposta, "Referrer-Policy"), caminho);
            assertEquals("camera=(), microphone=(), geolocation=()", cabecalho(resposta, "Permissions-Policy"),
                    caminho);
            assertEquals("DENY", cabecalho(resposta, "X-Frame-Options"), caminho);
            assertTrue(cabecalho(resposta, "Cache-Control").contains("no-store"), caminho);
            assertTrue(resposta.headers().firstValue("Server").isEmpty(), caminho);
        }
    }

    @Test
    void arquivosEstaticosSaoServidosComOTipoCerto() throws Exception {
        Http http = new Http(servidor);
        for (String caminho : new String[]{"/css/tokens.css", "/css/base.css"}) {
            HttpResponse<String> css = http.get(caminho);
            assertEquals(200, css.statusCode(), caminho);
            assertTrue(cabecalho(css, "Content-Type").startsWith("text/css"), caminho);
        }
        HttpResponse<String> logo = http.get("/img/logo.png");
        assertEquals(200, logo.statusCode());
        assertEquals("image/png", cabecalho(logo, "Content-Type"));
        assertEquals(404, http.get("/css/nao-existe.css").statusCode());
        assertTrue(http.get("/css/").statusCode() >= 400, "sem listagem de pasta");
    }

    @Test
    void telasEArquivosInternosNaoSaoServidosDireto() throws Exception {
        Http http = new Http(servidor);
        for (String caminho : new String[]{"/WEB-INF/jsp/erro.jsp", "/WEB-INF/tags/pagina.tag", "/web-inf/jsp/erro.jsp",
                "/WEB-INF/", "/META-INF/MANIFEST.MF", "/css/../WEB-INF/jsp/erro.jsp",
                "/css/%2e%2e/WEB-INF/jsp/erro.jsp",
                "/erro", "/x.jsp"}) {
            HttpResponse<String> resposta = http.get(caminho);
            assertTrue(resposta.statusCode() >= 400, caminho + " respondeu " + resposta.statusCode());
            assertFalse(resposta.body().contains("<%@"), caminho);
        }
    }

    @Test
    void enderecoAmbiguoERecusadoSemRepetirOEnderecoNemDetalheTecnico() throws Exception {
        Http http = new Http(servidor);
        for (String caminho : new String[]{"/css/%2e%2e/WEB-INF/jsp/erro.jsp", "/css/..%2fWEB-INF/jsp/erro.jsp",
                "/css/;/../WEB-INF/jsp/erro.jsp"}) {
            String resposta = http.bruto("127.0.0.1", caminho);
            assertTrue(resposta.startsWith("HTTP/1.1 400"), caminho + ": " + resposta);
            assertTrue(resposta.contains("Content-Security-Policy: " + CSP), caminho + ": " + resposta);
            for (String proibido : new String[]{"WEB-INF", "Ambiguous", "Jetty", "URI"}) {
                assertFalse(resposta.contains(proibido), proibido + " em " + caminho + ": " + resposta);
            }
        }
    }

    @Test
    void postSemTokenERecusadoComPaginaAmigavel() throws Exception {
        Http http = new Http(servidor);
        HttpResponse<String> semToken = http.post("/teste/formulario", "nome=Ana");
        assertEquals(403, semToken.statusCode());
        assertTrue(semToken.body().contains("Acesso restrito"), semToken.body());

        http.get("/teste/formulario");
        HttpResponse<String> tokenErrado = http.post("/teste/formulario", "_csrf=errado");
        assertEquals(403, tokenErrado.statusCode());
    }

    @Test
    void postComOTokenDaSessaoPassa() throws Exception {
        Http http = new Http(servidor);
        String token = http.get("/teste/formulario").body();
        assertTrue(token.length() >= 40, "token aleatório e longo: " + token);

        HttpResponse<String> resposta = http.post("/teste/formulario",
                "_csrf=" + URLEncoder.encode(token, StandardCharsets.UTF_8) + "&nome=Ana");
        assertEquals(200, resposta.statusCode());
        assertEquals("gravado", resposta.body());

        Http outraSessao = new Http(servidor);
        outraSessao.get("/teste/formulario");
        assertEquals(403, outraSessao.post("/teste/formulario", "_csrf=" + URLEncoder.encode(token,
                StandardCharsets.UTF_8)).statusCode(), "o token de uma sessão não vale em outra");
    }

    @Test
    void consultaSemFormularioNaoCriaSessao() throws Exception {
        HttpResponse<String> resposta = new Http(servidor).get("/saude");
        assertTrue(resposta.headers().allValues("Set-Cookie").isEmpty(), "sem cookie de sessão");
        HttpResponse<String> erro = new Http(servidor).get("/nao-existe");
        assertTrue(erro.headers().allValues("Set-Cookie").isEmpty(), "a página de erro não cria sessão");
    }

    @Test
    void aceitaSomenteNomesDoProprioComputador() throws Exception {
        Http http = new Http(servidor);
        for (String host : new String[]{"127.0.0.1", "localhost", "LOCALHOST", "[::1]"}) {
            String resposta = http.bruto(host + ":" + servidor.porta());
            assertTrue(resposta.startsWith("HTTP/1.1 200"), host + ": " + resposta);
        }
    }

    @Test
    void recusaDominioExternoApontadoParaOComputador() throws Exception {
        Http http = new Http(servidor);
        for (String host : new String[]{"atacante.example", "127.0.0.1.atacante.example", "192.168.0.10"}) {
            String resposta = http.bruto(host);
            assertTrue(resposta.startsWith("HTTP/1.1 400"), host + ": " + resposta);
            assertTrue(resposta.contains("iniciador"), host + ": " + resposta);
            assertFalse(resposta.contains("\r\nok"), host);
        }
    }

    private static String cabecalho(HttpResponse<String> resposta, String nome) {
        return resposta.headers().firstValue(nome).orElse("(ausente)");
    }
}

package br.com.inpetu.hub.comum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ServidorTest {

    private static final HttpClient CLIENTE = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @Test
    @Timeout(30)
    void sobeNumaPortaLivreESaudeResponde200() throws Exception {
        try (Servidor servidor = Servidor.iniciar(0)) {
            assertTrue(servidor.porta() > 0, "a porta 0 pede uma porta livre ao sistema");

            HttpResponse<String> resposta = get(servidor, "/saude");

            assertEquals(200, resposta.statusCode());
            assertEquals("ok", resposta.body());
            assertTrue(resposta.headers().firstValue("Content-Type").orElse("").startsWith("text/plain"));
            assertEquals("no-store", resposta.headers().firstValue("Cache-Control").orElse(""));
        }
    }

    @Test
    @Timeout(30)
    void naoAnunciaONomeNemAVersaoDoServidor() throws Exception {
        try (Servidor servidor = Servidor.iniciar(0)) {
            HttpResponse<String> resposta = get(servidor, "/saude");

            assertTrue(resposta.headers().firstValue("Server").isEmpty(), "o cabeçalho Server revela o Jetty");
            assertTrue(resposta.headers().firstValue("X-Powered-By").isEmpty());
        }
    }

    @Test
    @Timeout(30)
    void caminhoDesconhecidoResponde404() throws Exception {
        try (Servidor servidor = Servidor.iniciar(0)) {
            assertEquals(404, get(servidor, "/nao-existe").statusCode());
        }
    }

    @Test
    @Timeout(30)
    void escutaSoNoEnderecoLocal() throws Exception {
        try (Servidor servidor = Servidor.iniciar(0)) {
            assertEquals("127.0.0.1", servidor.endereco());
        }
    }

    private static HttpResponse<String> get(Servidor servidor, String caminho) throws Exception {
        HttpRequest pedido = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + servidor.porta() + caminho))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        return CLIENTE.send(pedido, HttpResponse.BodyHandlers.ofString());
    }
}

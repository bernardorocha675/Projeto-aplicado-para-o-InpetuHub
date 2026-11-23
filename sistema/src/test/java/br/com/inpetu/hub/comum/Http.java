package br.com.inpetu.hub.comum;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Cliente HTTP dos testes, com cookies (sessão) guardados entre os pedidos. */
final class Http {

    private final int porta;
    private final HttpClient cliente;

    Http(Servidor servidor) {
        this.porta = servidor.porta();
        this.cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    HttpResponse<String> get(String caminho) throws Exception {
        return enviar(pedido(caminho).GET().build());
    }

    HttpResponse<String> post(String caminho, String formulario) throws Exception {
        return enviar(pedido(caminho).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formulario))
                .build());
    }

    private HttpRequest.Builder pedido(String caminho) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + caminho))
                .timeout(Duration.ofSeconds(20));
    }

    private HttpResponse<String> enviar(HttpRequest pedido) throws Exception {
        return cliente.send(pedido, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    /** Pedido escrito à mão, para mandar um cabeçalho Host qualquer (o HttpClient não deixa). */
    String bruto(String host) throws Exception {
        return bruto(host, "/saude");
    }

    /** Pedido escrito à mão, com o caminho exatamente como veio (sem o cliente normalizar). */
    String bruto(String host, String caminho) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", porta)) {
            socket.setSoTimeout(10_000);
            OutputStream saida = socket.getOutputStream();
            saida.write(("GET " + caminho + " HTTP/1.1\r\nHost: " + host + "\r\nConnection: close\r\n\r\n")
                    .getBytes(StandardCharsets.ISO_8859_1));
            saida.flush();
            InputStream entrada = socket.getInputStream();
            ByteArrayOutputStream lido = new ByteArrayOutputStream();
            entrada.transferTo(lido);
            return lido.toString(StandardCharsets.UTF_8);
        }
    }
}

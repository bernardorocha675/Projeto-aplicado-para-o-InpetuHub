package br.com.inpetu.hub.comum;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URLEncoder;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Cliente HTTP dos testes, com cookies (sessão) guardados entre os pedidos. */
public final class Http {

    private final int porta;
    private final HttpClient cliente;
    private final CookieManager cookies = new CookieManager();

    public Http(Servidor servidor) {
        this.porta = servidor.porta();
        this.cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public HttpResponse<String> get(String caminho) throws Exception {
        return enviar(pedido(caminho).GET().build());
    }

    /** GET com um cabeçalho a mais. */
    public HttpResponse<String> get(String caminho, String cabecalho, String valor) throws Exception {
        return enviar(pedido(caminho).header(cabecalho, valor).GET().build());
    }

    public HttpResponse<String> post(String caminho, String formulario) throws Exception {
        return enviar(pedido(caminho).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formulario))
                .build());
    }

    /** Formulário com os campos na ordem dada (nome, valor, nome, valor...), já codificados. */
    public static String formulario(String... pares) {
        StringBuilder saida = new StringBuilder();
        for (int i = 0; i < pares.length; i += 2) {
            saida.append(i == 0 ? "" : "&")
                    .append(URLEncoder.encode(pares[i], StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(pares[i + 1], StandardCharsets.UTF_8));
        }
        return saida.toString();
    }

    /** O token CSRF escrito na tela (campo oculto {@code _csrf}). */
    public static String csrf(String html) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(
                html);
        if (!m.find()) {
            throw new AssertionError("Tela sem o campo _csrf.");
        }
        return m.group(1);
    }

    /** O valor do cookie de sessão guardado agora (null se não houver). */
    public String sessao() {
        for (HttpCookie cookie : cookies.getCookieStore().getCookies()) {
            if (cookie.getName().equals("JSESSIONID")) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /** Esquece os cookies, como um navegador novo. */
    public void esquecerCookies() {
        cookies.getCookieStore().removeAll();
    }

    private HttpRequest.Builder pedido(String caminho) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + caminho))
                .timeout(Duration.ofSeconds(20));
    }

    private HttpResponse<String> enviar(HttpRequest pedido) throws Exception {
        return cliente.send(pedido, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    /** Pedido escrito à mão, para mandar um cabeçalho Host qualquer (o HttpClient não deixa). */
    public String bruto(String host) throws Exception {
        return bruto(host, "/saude");
    }

    /** Pedido escrito à mão, com o caminho exatamente como veio (sem o cliente normalizar). */
    public String bruto(String host, String caminho) throws Exception {
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

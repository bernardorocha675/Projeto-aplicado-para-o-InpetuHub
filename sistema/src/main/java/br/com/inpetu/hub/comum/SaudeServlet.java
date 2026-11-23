package br.com.inpetu.hub.comum;

import java.io.IOException;
import java.io.Serial;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * GET /saude: responde "ok" quando o servidor está de pé. O iniciador usa para saber que pode abrir o navegador.
 */
public final class SaudeServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest pedido, HttpServletResponse resposta) throws IOException {
        resposta.setStatus(HttpServletResponse.SC_OK);
        resposta.setContentType("text/plain");
        resposta.setCharacterEncoding(StandardCharsets.UTF_8);
        resposta.setHeader("Cache-Control", "no-store");
        resposta.getWriter().write("ok");
    }
}

package br.com.inpetu.hub.comum;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Os mesmos cabeçalhos de segurança do sistema antigo (Spring Security), em todas as respostas. */
final class Cabecalhos extends HttpFilter {

    static final String CSP = "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:;"
            + " connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'";

    private static final long serialVersionUID = 1L;

    static final Map<String, String> TODOS = todos();

    private static Map<String, String> todos() {
        Map<String, String> valores = new LinkedHashMap<>();
        valores.put("Content-Security-Policy", CSP);
        valores.put("X-Content-Type-Options", "nosniff");
        valores.put("X-Frame-Options", "DENY");
        valores.put("Referrer-Policy", "no-referrer");
        valores.put("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        valores.put("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        valores.put("Pragma", "no-cache");
        valores.put("Expires", "0");
        return Map.copyOf(valores);
    }

    static void aplicar(HttpServletResponse resposta) {
        TODOS.forEach(resposta::setHeader);
    }

    @Override
    protected void doFilter(HttpServletRequest pedido, HttpServletResponse resposta, FilterChain cadeia)
            throws IOException, ServletException {
        aplicar(resposta);
        cadeia.doFilter(pedido, resposta);
    }
}

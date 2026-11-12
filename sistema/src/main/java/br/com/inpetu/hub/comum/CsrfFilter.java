package br.com.inpetu.hub.comum;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * CSRF por token guardado na sessão: todo pedido que muda dados (POST e afins) precisa mandar o token no campo
 * {@code _csrf} ou no cabeçalho {@code X-CSRF-Token}. O token só é criado (com a sessão) quando uma tela o usa.
 */
public final class CsrfFilter extends HttpFilter {

    /** Atributo da requisição com o token; nas telas: {@code <input type="hidden" name="_csrf" value="${csrf}">}. */
    public static final String ATRIBUTO = "csrf";
    static final String CAMPO = "_csrf";
    static final String CABECALHO = "X-CSRF-Token";
    private static final String NA_SESSAO = "inpetu.csrf";
    private static final Set<String> SEGUROS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final long serialVersionUID = 1L;

    @Override
    protected void doFilter(HttpServletRequest pedido, HttpServletResponse resposta, FilterChain cadeia)
            throws IOException, ServletException {
        if (!SEGUROS.contains(pedido.getMethod()) && !confere(pedido)) {
            resposta.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        pedido.setAttribute(ATRIBUTO, new Token(pedido));
        cadeia.doFilter(pedido, resposta);
    }

    private static boolean confere(HttpServletRequest pedido) {
        HttpSession sessao = pedido.getSession(false);
        Object esperado = sessao == null ? null : sessao.getAttribute(NA_SESSAO);
        String recebido = pedido.getHeader(CABECALHO);
        if (recebido == null) {
            recebido = pedido.getParameter(CAMPO);
        }
        return esperado instanceof String token && recebido != null && MessageDigest.isEqual(token.getBytes(
                StandardCharsets.UTF_8), recebido.getBytes(StandardCharsets.UTF_8));
    }

    /** O token da sessão, criado na primeira vez que uma tela o escreve. */
    private record Token(HttpServletRequest pedido) {

        @Override
        public String toString() {
            HttpSession sessao = pedido.getSession(true);
            synchronized (sessao) {
                Object atual = sessao.getAttribute(NA_SESSAO);
                if (atual instanceof String token) {
                    return token;
                }
                byte[] bytes = new byte[32];
                ALEATORIO.nextBytes(bytes);
                String novo = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                sessao.setAttribute(NA_SESSAO, novo);
                return novo;
            }
        }
    }
}

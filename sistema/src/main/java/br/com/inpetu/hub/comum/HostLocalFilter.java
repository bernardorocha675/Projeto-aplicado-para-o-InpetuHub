package br.com.inpetu.hub.comum;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A instalação local só escuta no próprio computador. Recusar outros nomes de host impede que uma página externa aponte
 * o domínio dela para 127.0.0.1 (DNS rebinding) e use as contas de demonstração.
 */
final class HostLocalFilter extends HttpFilter {

    private static final long serialVersionUID = 1L;
    static final Set<String> NOMES_LOCAIS = Set.of("127.0.0.1", "localhost", "[::1]", "::1");

    @Override
    protected void doFilter(HttpServletRequest pedido, HttpServletResponse resposta, FilterChain cadeia)
            throws IOException, ServletException {
        String host = pedido.getServerName();
        if (host == null || !NOMES_LOCAIS.contains(host.toLowerCase(Locale.ROOT))) {
            resposta.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resposta.setContentType("text/plain;charset=UTF-8");
            resposta.getWriter().write("Endereço recusado. Abra o sistema pelo iniciador.");
            return;
        }
        cadeia.doFilter(pedido, resposta);
    }
}

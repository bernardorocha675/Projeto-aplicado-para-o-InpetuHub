package br.com.inpetu.hub.solicitacao;

import java.io.IOException;
import java.io.Serial;
import java.time.Duration;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.ErroServlet;
import br.com.inpetu.hub.comum.Limites;
import br.com.inpetu.hub.comum.Telas;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Envios do pedido público: 15 a cada 15 minutos por endereço, como no antigo. */
final class LimitePedidosFilter extends HttpFilter {

    private static final Duration JANELA = Duration.ofMinutes(15);

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Limites limites;

    LimitePedidosFilter(Ambiente ambiente) {
        this.limites = new Limites(ambiente.relogio());
    }

    @Override
    protected void doFilter(HttpServletRequest pedido, HttpServletResponse resposta, FilterChain cadeia)
            throws IOException, ServletException {
        if ("POST".equals(pedido.getMethod()) && Telas.caminho(pedido).startsWith("/reservas/") && !limites.permite(
                "pedido:" + Limites.resumo(pedido.getRemoteAddr()), 15, JANELA)) {
            pedido.setAttribute(ErroServlet.ESPERA, JANELA.toSeconds());
            resposta.sendError(429);
            return;
        }
        cadeia.doFilter(pedido, resposta);
    }
}

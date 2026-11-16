package br.com.inpetu.hub.conta;

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

/**
 * Limites de tentativas, os mesmos do antigo. Login: fora do modo local, 60 por endereço e 10 por conta a cada 15 min
 * (no modo local, sem limite, como no antigo). Troca de senha e demais formulários da conta: 12 a cada 5 min. As chaves
 * guardam só um resumo do usuário e do endereço.
 */
final class LimiteFilter extends HttpFilter {

    private static final Duration JANELA_LOGIN = Duration.ofMinutes(15);
    private static final Duration JANELA_CONTA = Duration.ofMinutes(5);

    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean modoLocal;
    private final transient Limites limites;

    LimiteFilter(Ambiente ambiente) {
        this.modoLocal = ambiente.modoLocal();
        this.limites = new Limites(ambiente.relogio());
    }

    @Override
    protected void doFilter(HttpServletRequest pedido, HttpServletResponse resposta, FilterChain cadeia)
            throws IOException, ServletException {
        if ("POST".equals(pedido.getMethod())) {
            String caminho = Telas.caminho(pedido);
            String endereco = Limites.resumo(pedido.getRemoteAddr());
            Duration espera = null;
            if (caminho.equals("/login") && !modoLocal) {
                String conta = Limites.resumo(NomesDeLogin.normalizar(pedido.getParameter("usuario")));
                if (!limites.permite("login-endereco:" + endereco, 60, JANELA_LOGIN) || !limites.permite(
                        "login-conta:" + conta, 10, JANELA_LOGIN)) {
                    espera = JANELA_LOGIN;
                }
            } else if (caminho.startsWith("/conta/") && !limites.permite("conta:" + endereco, 12, JANELA_CONTA)) {
                espera = JANELA_CONTA;
            }
            if (espera != null) {
                pedido.setAttribute(ErroServlet.ESPERA, espera.toSeconds());
                resposta.sendError(429);
                return;
            }
        }
        cadeia.doFilter(pedido, resposta);
    }
}

package br.com.inpetu.hub.comum;

import java.io.IOException;
import java.util.UUID;

import br.com.inpetu.hub.comum.log.ContextoRequisicao;
import br.com.inpetu.hub.comum.log.Eventos;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Primeiro filtro: dá um identificador à requisição (cabeçalho X-Request-ID e log), registra uma linha no fim e, se a
 * requisição terminar numa exceção, registra a causa sem a mensagem.
 */
final class RequisicaoFilter extends HttpFilter {

    static final String ATRIBUTO_ID = "inpetu.requestId";
    private static final long serialVersionUID = 1L;

    @Override
    protected void doFilter(HttpServletRequest pedido, HttpServletResponse resposta, FilterChain cadeia)
            throws IOException, ServletException {
        String id = UUID.randomUUID().toString();
        ContextoRequisicao.definir(id);
        pedido.setAttribute(ATRIBUTO_ID, id);
        resposta.setHeader("X-Request-ID", id);
        long inicio = System.nanoTime();
        int status = 0;
        Throwable falha = null;
        try {
            cadeia.doFilter(pedido, resposta);
        } catch (IOException | ServletException | RuntimeException | Error erro) {
            falha = erro;
            status = Erros.daExcecao(erro).status();
            Eventos.problema(pedido.getMethod(), pedido.getRequestURI(), status, erro);
            throw erro;
        } finally {
            try {
                Eventos.requisicao(pedido.getMethod(), pedido.getRequestURI(), falha == null
                        ? resposta.getStatus()
                        : status, (System.nanoTime() - inicio) / 1_000_000, status >= 500 ? falha : null);
            } finally {
                ContextoRequisicao.limpar();
            }
        }
    }
}

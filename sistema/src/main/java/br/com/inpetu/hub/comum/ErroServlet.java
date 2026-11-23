package br.com.inpetu.hub.comum;

import java.io.IOException;
import java.io.Serial;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Página de erro de todo o sistema (JSP), sem detalhe técnico, com a referência para suporte. */
public final class ErroServlet extends HttpServlet {

    static final String CAMINHO = "/erro";
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    protected void service(HttpServletRequest pedido, HttpServletResponse resposta) throws IOException,
            ServletException {
        if (pedido.getDispatcherType() != DispatcherType.ERROR) {
            resposta.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Throwable falha = (Throwable) pedido.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object situacao = pedido.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Erros.Tela tela = falha != null
                ? Erros.daExcecao(falha)
                : Erros.daSituacao(situacao instanceof Integer s ? s : 500);
        resposta.setStatus(tela.status());
        Cabecalhos.aplicar(resposta);
        Object id = pedido.getAttribute(RequisicaoFilter.ATRIBUTO_ID);
        if (id != null) {
            resposta.setHeader("X-Request-ID", id.toString());
        }
        pedido.setAttribute("titulo", tela.titulo());
        pedido.setAttribute("mensagem", tela.mensagem());
        pedido.setAttribute("referencia", tela.status() >= 500 ? id : null);
        pedido.getRequestDispatcher("/WEB-INF/jsp/erro.jsp").forward(pedido, resposta);
    }
}

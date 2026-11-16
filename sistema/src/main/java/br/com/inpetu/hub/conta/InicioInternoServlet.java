package br.com.inpetu.hub.conta;

import java.io.IOException;
import java.io.Serial;

import br.com.inpetu.hub.comum.Telas;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Início da Área Interna (provisório: o painel com reservas e solicitações vem na entrega 8). */
final class InicioInternoServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest pedido, HttpServletResponse resposta) throws ServletException,
            IOException {
        ContaSessao conta = (ContaSessao) pedido.getAttribute(SessaoFilter.NO_PEDIDO);
        pedido.setAttribute("nomeDaConta", conta.nome());
        pedido.setAttribute("perfilDaConta", conta.perfil() == Perfil.ADMIN ? "Administrador" : "Colaborador");
        Telas.mostrar(pedido, resposta, "interna/inicio");
    }
}

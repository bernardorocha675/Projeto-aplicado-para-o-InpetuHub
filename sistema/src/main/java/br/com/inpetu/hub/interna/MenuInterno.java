package br.com.inpetu.hub.interna;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.conta.ContaSessao;
import br.com.inpetu.hub.reserva.ReservaConsultas;
import jakarta.servlet.http.HttpServletRequest;

/** Prepara o menu da Área Interna: o item atual, o nome da conta e se há pedidos aguardando análise. */
public final class MenuInterno {

    private MenuInterno() {
    }

    public static void preparar(HttpServletRequest pedido, Ambiente ambiente, String atual) {
        ContaSessao conta = ContaSessao.doPedido(pedido);
        pedido.setAttribute("menuAtual", atual);
        pedido.setAttribute("paginaAtual", "interna");
        pedido.setAttribute("nomeDaConta", conta == null ? "" : conta.nome());
        pedido.setAttribute("pedidosPendentes", new ReservaConsultas(ambiente.banco()).contar(
                "select count(*) from solicitacao_publica where estado = 'RECEBIDA'"));
    }
}

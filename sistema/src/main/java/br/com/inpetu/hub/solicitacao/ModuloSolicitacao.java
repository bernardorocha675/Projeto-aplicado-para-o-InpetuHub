package br.com.inpetu.hub.solicitacao;

import java.util.EnumSet;

import org.eclipse.jetty.ee11.servlet.FilterHolder;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;

import br.com.inpetu.hub.comum.Ambiente;
import jakarta.servlet.DispatcherType;

/** Liga o pedido público, o limite de envios e o vencimento dos pedidos ao servidor. */
public final class ModuloSolicitacao {

    private ModuloSolicitacao() {
    }

    public static void registrar(ServletContextHandler contexto, Ambiente ambiente) {
        contexto.addFilter(new FilterHolder(new LimitePedidosFilter(ambiente)), "/*", EnumSet.of(
                DispatcherType.REQUEST));
        ServletHolder pedido = new ServletHolder(new PedidoServlet(ambiente));
        contexto.addServlet(pedido, "/reserva");
        contexto.addServlet(pedido, "/reservas/*");
        contexto.addBean(new VencimentoDePedidos(ambiente), true);
    }
}

package br.com.inpetu.hub.conta;

import java.util.EnumSet;

import org.eclipse.jetty.ee11.servlet.FilterHolder;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;

import br.com.inpetu.hub.comum.Ambiente;
import jakarta.servlet.DispatcherType;

/** Liga login, sessão, limites e troca de senha ao servidor, depois dos filtros de segurança web. */
public final class ModuloConta {

    private ModuloConta() {
    }

    public static void registrar(ServletContextHandler contexto, Ambiente ambiente) {
        EnumSet<DispatcherType> pedido = EnumSet.of(DispatcherType.REQUEST);
        contexto.addFilter(new FilterHolder(new LimiteFilter(ambiente)), "/*", pedido);
        contexto.addFilter(new FilterHolder(new SessaoFilter(ambiente)), "/*", pedido);
        contexto.addServlet(new ServletHolder(new LoginServlet(ambiente)), "/login");
        contexto.addServlet(new ServletHolder(new SairServlet(ambiente)), "/sair");
        contexto.addServlet(new ServletHolder(new SenhaServlet(ambiente)), "/conta/senha");
        contexto.addServlet(new ServletHolder(new InicioInternoServlet()), "/admin");
    }
}

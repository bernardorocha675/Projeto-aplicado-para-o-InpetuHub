package br.com.inpetu.hub.interna;

import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;

import br.com.inpetu.hub.comum.Ambiente;

/** Liga o painel, as solicitações e as reservas da Área Interna ao servidor. */
public final class ModuloInterna {

    private ModuloInterna() {
    }

    public static void registrar(ServletContextHandler contexto, Ambiente ambiente) {
        contexto.addServlet(new ServletHolder(new PainelServlet(ambiente)), "/admin");
        ServletHolder solicitacoes = new ServletHolder(new SolicitacoesServlet(ambiente));
        contexto.addServlet(solicitacoes, SolicitacoesServlet.RAIZ);
        contexto.addServlet(solicitacoes, SolicitacoesServlet.RAIZ + "/*");
        ServletHolder reservas = new ServletHolder(new ReservasServlet(ambiente));
        contexto.addServlet(reservas, ReservasServlet.RAIZ);
        contexto.addServlet(reservas, ReservasServlet.RAIZ + "/*");
    }
}

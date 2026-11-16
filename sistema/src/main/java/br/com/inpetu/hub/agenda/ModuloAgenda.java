package br.com.inpetu.hub.agenda;

import java.io.Serial;

import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.Telas;
import br.com.inpetu.hub.espaco.EspacosServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Liga a agenda pública, /disponibilidade e o cadastro de espaços ao servidor. */
public final class ModuloAgenda {

    private ModuloAgenda() {
    }

    public static void registrar(ServletContextHandler contexto, Ambiente ambiente) {
        contexto.addServlet(new ServletHolder(new AgendaServlet(ambiente)), "/agenda");
        contexto.addServlet(new ServletHolder(new DisponibilidadeServlet(ambiente)), "/disponibilidade");
        // "" é só o endereço raiz (/): por enquanto o início abre a agenda.
        contexto.addServlet(new ServletHolder(new Inicio()), "");
        ServletHolder espacos = new ServletHolder(new EspacosServlet(ambiente));
        contexto.addServlet(espacos, EspacosServlet.RAIZ);
        contexto.addServlet(espacos, EspacosServlet.RAIZ + "/*");
    }

    private static final class Inicio extends HttpServlet {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest pedido, HttpServletResponse resposta) {
            Telas.redirecionar(resposta, "/agenda");
        }
    }
}

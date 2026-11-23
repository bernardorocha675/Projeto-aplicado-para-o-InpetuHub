package br.com.inpetu.hub.comum;

import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

/**
 * Jetty embutido, escutando só em 127.0.0.1. A porta 0 pede uma porta livre ao sistema.
 */
public final class Servidor implements AutoCloseable {

    public static final String ENDERECO = "127.0.0.1";

    private final Server jetty;
    private final ServerConnector conector;

    private Servidor(Server jetty, ServerConnector conector) {
        this.jetty = jetty;
        this.conector = conector;
    }

    public static Servidor iniciar(int porta) throws Exception {
        if (porta < 0 || porta > 65535) {
            throw new IllegalArgumentException("Porta fora do intervalo 0–65535: " + porta);
        }
        Server jetty = new Server();
        jetty.setStopAtShutdown(true);

        HttpConfiguration http = new HttpConfiguration();
        http.setSendServerVersion(false);
        http.setSendXPoweredBy(false);
        ServerConnector conector = new ServerConnector(jetty, new HttpConnectionFactory(http));
        conector.setHost(ENDERECO);
        conector.setPort(porta);
        jetty.addConnector(conector);

        ServletContextHandler contexto = new ServletContextHandler();
        contexto.setContextPath("/");
        contexto.addServlet(SaudeServlet.class, "/saude");
        jetty.setHandler(contexto);

        try {
            jetty.start();
        } catch (Exception erro) {
            jetty.stop();
            throw erro;
        }
        return new Servidor(jetty, conector);
    }

    public int porta() {
        return conector.getLocalPort();
    }

    public String endereco() {
        return conector.getHost();
    }

    public void aguardar() throws InterruptedException {
        jetty.join();
    }

    @Override
    public void close() {
        try {
            jetty.stop();
        } catch (InterruptedException erro) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Encerramento do servidor interrompido.", erro);
        } catch (Exception erro) {
            throw new IllegalStateException("Falha ao encerrar o servidor.", erro);
        }
    }
}

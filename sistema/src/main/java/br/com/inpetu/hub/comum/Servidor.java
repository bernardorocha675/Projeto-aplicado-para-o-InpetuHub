package br.com.inpetu.hub.comum;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.ee11.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.ee11.jsp.JettyJspServlet;
import org.eclipse.jetty.ee11.servlet.DefaultServlet;
import org.eclipse.jetty.ee11.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.ee11.servlet.FilterHolder;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.ResourceFactory;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServlet;

/**
 * Jetty embutido, escutando só em 127.0.0.1 (a porta 0 pede uma porta livre). Telas em JSP, arquivos estáticos e os
 * filtros de segurança, na ordem: requisição/log, cabeçalhos, host local, CSRF.
 */
public final class Servidor implements AutoCloseable {

    public static final String ENDERECO = "127.0.0.1";
    private static final String[] PASTAS_ESTATICAS = {"/css/*", "/js/*", "/img/*", "/fontes/*"};

    private final Server jetty;
    private final ServerConnector conector;
    private final Path rascunhoJsp;

    private Servidor(Server jetty, ServerConnector conector, Path rascunhoJsp) {
        this.jetty = jetty;
        this.conector = conector;
        this.rascunhoJsp = rascunhoJsp;
    }

    public static Servidor iniciar(int porta) throws Exception {
        return iniciar(porta, Map.of());
    }

    /** Sobe o servidor com as rotas do sistema e as rotas extras (caminho → servlet). */
    public static Servidor iniciar(int porta, Map<String, ? extends HttpServlet> rotas) throws Exception {
        if (porta < 0 || porta > 65535) {
            throw new IllegalArgumentException("Porta fora do intervalo 0–65535: " + porta);
        }
        Server jetty = new Server();
        jetty.setStopAtShutdown(true);
        jetty.setErrorHandler(new ErroDoServidor());

        HttpConfiguration http = new HttpConfiguration();
        http.setSendServerVersion(false);
        http.setSendXPoweredBy(false);
        ServerConnector conector = new ServerConnector(jetty, new HttpConnectionFactory(http));
        conector.setHost(ENDERECO);
        conector.setPort(porta);
        jetty.addConnector(conector);

        Path rascunho = Files.createTempDirectory("inpetu-jsp-");
        ServletContextHandler contexto = new ServletContextHandler(ServletContextHandler.SESSIONS);
        contexto.setContextPath("/");
        contexto.getSessionHandler().setHttpOnly(true);
        contexto.setBaseResource(ResourceFactory.of(contexto).newResource(webapp().toURI()));
        // Telas (.jsp), modelos (.tag) e o manifesto nunca são servidos direto.
        contexto.setProtectedTargets(new String[]{"/WEB-INF", "/META-INF"});
        configurarJsp(contexto, rascunho);
        configurarFiltros(contexto);
        configurarErros(contexto);

        ServletHolder estaticos = new ServletHolder("estaticos", DefaultServlet.class);
        estaticos.setInitParameter("dirAllowed", "false");
        // Mapeado em /css/*, /img/*...: o arquivo é procurado pelo caminho inteiro, não só pelo que vem depois.
        estaticos.setInitParameter("pathInfoOnly", "false");
        for (String pasta : PASTAS_ESTATICAS) {
            contexto.addServlet(estaticos, pasta);
        }
        contexto.addServlet(SaudeServlet.class, "/saude");
        rotas.forEach((caminho, servlet) -> contexto.addServlet(new ServletHolder(servlet), caminho));
        jetty.setHandler(contexto);

        try {
            jetty.start();
        } catch (Exception erro) {
            jetty.stop();
            apagar(rascunho);
            throw erro;
        }
        return new Servidor(jetty, conector, rascunho);
    }

    /** A pasta webapp/ dentro do jar (ou de target/classes no desenvolvimento). */
    private static URL webapp() {
        URL url = Servidor.class.getClassLoader().getResource("webapp/");
        if (url == null) {
            throw new IllegalStateException("Pasta webapp/ não encontrada no classpath.");
        }
        return url;
    }

    private static void configurarJsp(ServletContextHandler contexto, Path rascunho) {
        contexto.setAttribute("jakarta.servlet.context.tempdir", rascunho.toFile());
        contexto.setClassLoader(Servidor.class.getClassLoader());
        contexto.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
        contexto.addServletContainerInitializer(new JettyJasperInitializer());
        ServletHolder jsp = new ServletHolder("jsp", JettyJspServlet.class);
        jsp.setInitOrder(0);
        jsp.setInitParameter("compilerSourceVM", "21");
        jsp.setInitParameter("compilerTargetVM", "21");
        jsp.setInitParameter("trimSpaces", "false");
        contexto.addServlet(jsp, "*.jsp");
    }

    private static void configurarFiltros(ServletContextHandler contexto) {
        EnumSet<DispatcherType> pedido = EnumSet.of(DispatcherType.REQUEST);
        contexto.addFilter(new FilterHolder(new RequisicaoFilter()), "/*", pedido);
        contexto.addFilter(new FilterHolder(new Cabecalhos()), "/*", pedido);
        contexto.addFilter(new FilterHolder(new HostLocalFilter()), "/*", pedido);
        contexto.addFilter(new FilterHolder(new CsrfFilter()), "/*", pedido);
    }

    private static void configurarErros(ServletContextHandler contexto) {
        ErrorPageErrorHandler erros = new ErrorPageErrorHandler();
        erros.setShowStacks(false);
        erros.setShowMessageInTitle(false);
        erros.addErrorPage(Throwable.class, ErroServlet.CAMINHO);
        erros.addErrorPage(400, 599, ErroServlet.CAMINHO);
        contexto.setErrorHandler(erros);
        contexto.addServlet(ErroServlet.class, ErroServlet.CAMINHO);
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
        } finally {
            apagar(rascunhoJsp);
        }
    }

    /** As páginas compiladas ficam numa pasta temporária, apagada ao encerrar. */
    private static void apagar(Path pasta) {
        if (!Files.exists(pasta)) {
            return;
        }
        try (Stream<Path> arquivos = Files.walk(pasta)) {
            for (Path arquivo : arquivos.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(arquivo);
            }
        } catch (IOException erro) {
            throw new UncheckedIOException("Não foi possível apagar a pasta temporária das telas.", erro);
        }
    }

}

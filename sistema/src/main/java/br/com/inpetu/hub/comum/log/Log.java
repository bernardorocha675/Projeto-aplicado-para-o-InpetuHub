package br.com.inpetu.hub.comum.log;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/** Liga o log do sistema: JUL com o formato seguro, na saída de erro (o arquivo vem com o iniciador). */
public final class Log {

    private Log() {
    }

    public static void configurar() {
        // O Jetty fala com o SLF4J, que não tem implementação aqui (decisão 3b): sem isto ele avisa a cada subida.
        System.setProperty("slf4j.internal.verbosity", "ERROR");
        LogManager.getLogManager().reset();
        Logger raiz = Logger.getLogger("");
        Handler console = new ConsoleHandler();
        console.setFormatter(new LogSeguro());
        console.setLevel(Level.INFO);
        raiz.addHandler(console);
        raiz.setLevel(Level.INFO);
    }
}

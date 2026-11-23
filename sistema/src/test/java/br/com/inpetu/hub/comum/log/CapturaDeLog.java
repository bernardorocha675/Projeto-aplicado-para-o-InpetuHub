package br.com.inpetu.hub.comum.log;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/** Guarda, já formatadas pelo LogSeguro, as linhas que os eventos do sistema gravariam. */
public final class CapturaDeLog extends Handler implements AutoCloseable {

    private final Logger logger = Logger.getLogger(Eventos.class.getName());
    private final LogSeguro formato = new LogSeguro();
    private final List<String> linhas = new ArrayList<>();

    public CapturaDeLog() {
        setLevel(Level.ALL);
        logger.addHandler(this);
    }

    @Override
    public synchronized void publish(LogRecord registro) {
        linhas.add(formato.format(registro));
    }

    public synchronized String texto() {
        return String.join("", linhas);
    }

    /** O log da requisição sai no fim dela, às vezes logo depois de a resposta chegar ao cliente. */
    public String aguardar(String trecho) throws InterruptedException {
        long limite = System.nanoTime() + 5_000_000_000L;
        while (!texto().contains(trecho) && System.nanoTime() < limite) {
            Thread.sleep(20);
        }
        return texto();
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
        logger.removeHandler(this);
    }
}

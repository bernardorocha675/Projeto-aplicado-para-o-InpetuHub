package br.com.inpetu.hub.comum.log;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Os únicos eventos do sistema que vão para o log com texto: todos montados com campos controlados. Nunca recebe
 * endereço livre, formulário, login, senha nem mensagem de exceção.
 */
public final class Eventos {

    private static final Logger LOG = Logger.getLogger(Eventos.class.getName());

    private Eventos() {
    }

    public static void servidorPronto() {
        LOG.info("event=SERVER_READY");
    }

    public static void servidorParado() {
        LOG.info("event=SERVER_STOPPED");
    }

    /** O sistema não iniciou: a categoria vem de FalhaCausa; a cadeia de causas sai pelo LogSeguro. */
    public static void inicioFalhou(Throwable falha) {
        LOG.log(Level.SEVERE, "event=SERVER_START_FAILED failure=" + FalhaCausa.de(falha).categoria(), falha);
    }

    /** Uma linha por requisição: método, rota canônica, situação e duração. */
    public static void requisicao(String metodo, String caminho, int status, long milissegundos, Throwable falha) {
        String texto = "event=HTTP method=" + metodo(metodo) + " route=" + RotasSeguras.canonica(caminho) + " status="
                + status + " durationMs=" + milissegundos;
        Level nivel = falha != null || status >= 500
                ? Level.SEVERE
                : status >= 400 || milissegundos >= 1000 ? Level.WARNING : Level.INFO;
        LOG.log(nivel, texto);
    }

    /** Requisição que terminou numa exceção: recusada (4xx, aviso) ou falha (5xx, erro), com a causa. */
    public static void problema(String metodo, String caminho, int status, Throwable falha) {
        String texto = "event=" + (status >= 500 ? "REQUEST_FAILED" : "REQUEST_REJECTED") + " method=" + metodo(
                metodo) + " route=" + RotasSeguras.canonica(caminho) + " status=" + status;
        LOG.log(status >= 500 ? Level.SEVERE : Level.WARNING, texto, falha);
    }

    private static String metodo(String metodo) {
        return switch (metodo == null ? "" : metodo) {
            case "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS" -> metodo;
            default -> "OTHER";
        };
    }
}

package br.com.inpetu.hub.comum.log;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

    /** Espelho da auditoria gravada no banco: só a ação, o alvo e o resultado, nomes escritos no código. */
    public static void auditoria(String acao, String alvo, String resultado) {
        String texto = "event=AUDIT action=" + palavra(acao) + " target=" + palavra(alvo) + " result=" + palavra(
                resultado);
        LOG.log("NEGADO".equals(resultado) ? Level.WARNING : Level.INFO, texto);
    }

    /** O que aconteceu na página: só códigos de uma lista (o servlet confere), a tela canônica e a linha do script. */
    public static void tela(String tipo, String alvo, String pagina, int linha) {
        LOG.log("CLIQUE".equals(tipo) ? Level.INFO : Level.WARNING, "event=SCREEN type=" + palavra(tipo) + " target="
                + nome(alvo) + " page=" + RotasSeguras.canonica(pagina) + (linha > 0 ? " line=" + linha : ""));
    }

    /** Campos que o navegador recusou antes de enviar (o servidor nem recebe o formulário). */
    public static void camposRecusados(Collection<String> campos, String pagina) {
        LOG.warning("event=SCREEN type=CAMPO_INVALIDO fields=" + nomes(campos) + " page=" + RotasSeguras.canonica(
                pagina));
    }

    /** Formulário recusado: só os nomes dos campos, nunca o que foi digitado. */
    public static void formularioRecusado(String formulario, Collection<String> campos) {
        LOG.warning("event=FORM_REJECTED form=" + palavra(formulario) + " fields=" + nomes(campos));
    }

    private static String nomes(Collection<String> campos) {
        return campos.stream().limit(20).map(Eventos::palavra).collect(Collectors.joining(","));
    }

    /** Regra que recusou: o ponto do código, nunca a mensagem. */
    public static void regraRecusou(String formulario, Throwable regra) {
        StackTraceElement[] pilha = regra.getStackTrace();
        String ponto = "OTHER";
        if (pilha.length > 0) {
            String classe = pilha[0].getClassName();
            String texto = classe.substring(classe.lastIndexOf('.') + 1) + "." + pilha[0].getMethodName() + ":"
                    + pilha[0].getLineNumber();
            ponto = texto.matches("[A-Za-z]{1,60}\\.[A-Za-z]{1,60}:[0-9]{1,6}") ? texto : "OTHER";
        }
        LOG.log(Level.WARNING, "event=RULE_REJECTED form=" + palavra(formulario) + " rule=" + ponto);
    }

    /** Uma tarefa periódica falhou: só o nome da tarefa e a causa (sem mensagem). */
    public static void falhaEmTarefa(String tarefa, Throwable falha) {
        LOG.log(Level.SEVERE, "event=TASK_FAILED task=" + palavra(tarefa), falha);
    }

    // Os nomes vêm de constantes do código; ainda assim, qualquer coisa fora do formato vira OTHER.
    private static String palavra(String valor) {
        return valor != null && valor.matches("[A-Za-z_]{1,40}") ? valor : "OTHER";
    }

    private static String nome(String valor) {
        return valor != null && valor.matches("[A-Za-z_]{1,40}(-[a-z]{1,20})?(\\.js)?") ? valor : "OTHER";
    }

    private static String metodo(String metodo) {
        return switch (metodo == null ? "" : metodo) {
            case "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS" -> metodo;
            default -> "OTHER";
        };
    }
}

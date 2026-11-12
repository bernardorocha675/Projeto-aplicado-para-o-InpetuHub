package br.com.inpetu.hub.comum.log;

import java.time.Instant;
import java.util.List;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * JSON por linha. Só os textos de {@link Eventos} entram; mensagens de bibliotecas e de exceções nunca são serializadas
 * (trazem caminhos, SQL com valores e dados de formulário).
 */
public final class LogSeguro extends Formatter {

    private static final String VERSAO = "1\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(-(alpha|beta|rc)\\.[0-9]{1,4}|-SNAPSHOT)?";
    private static final int MAX_LINHAS = 20;
    private static final int MAX_LINHAS_RAIZ = 10;

    @Override
    public String format(LogRecord registro) {
        String id = ContextoRequisicao.atual();
        if (id == null || !id.matches("[a-f0-9-]{36}")) {
            id = "-";
        }
        String mensagem = Eventos.class.getName().equals(registro.getLoggerName()) && registro.getMessage() != null
                ? registro.getMessage()
                : "FRAMEWORK_EVENT";
        String sessao = System.getProperty("inpetu.session.id", "");
        StringBuilder saida = new StringBuilder("{\"time\":").append(texto(Instant.ofEpochMilli(registro.getMillis())
                .toString()))
                .append(",\"appVersion\":")
                .append(texto(versao(System.getProperty("inpetu.app.version", LogSeguro.class.getPackage()
                        .getImplementationVersion()))))
                .append(",\"launcherVersion\":")
                .append(texto(versao(System.getProperty("inpetu.launcher.version"))))
                .append(",\"sessionId\":")
                .append(texto(sessao.matches("[a-f0-9]{32}") ? sessao : "standalone"))
                .append(",\"level\":")
                .append(texto(nivel(registro.getLevel())))
                .append(",\"logger\":")
                .append(texto(nomeDoLogger(registro.getLoggerName())))
                .append(",\"requestId\":")
                .append(texto(id))
                .append(",\"message\":")
                .append(texto(mensagem));
        if (registro.getThrown() != null) {
            excecao(saida, registro.getThrown());
        }
        return saida.append("}\n").toString();
    }

    private static void excecao(StringBuilder saida, Throwable falha) {
        FalhaCausa causa = FalhaCausa.de(falha);
        List<Throwable> cadeia = causa.cadeia();
        saida.append(",\"exceptionType\":").append(texto(falha.getClass().getName()));
        linhas(saida, "frames", falha, MAX_LINHAS);
        if (cadeia.size() > 1) {
            saida.append(",\"causes\":[");
            for (int i = 1; i < cadeia.size(); i++) {
                saida.append(i == 1 ? "" : ",").append(texto(cadeia.get(i).getClass().getName()));
            }
            saida.append(']');
            linhas(saida, "rootFrames", cadeia.getLast(), MAX_LINHAS_RAIZ);
        }
        saida.append(",\"failure\":").append(texto(causa.categoria()));
        if (causa.sqlState() != null) {
            saida.append(",\"sqlState\":").append(texto(causa.sqlState()));
        }
        if (causa.codigoSql() != null) {
            saida.append(",\"sqlErrorCode\":").append(causa.codigoSql());
        }
    }

    private static void linhas(StringBuilder saida, String nome, Throwable falha, int maximo) {
        saida.append(",\"").append(nome).append("\":[");
        StackTraceElement[] pilha = falha.getStackTrace();
        for (int i = 0; i < Math.min(pilha.length, maximo); i++) {
            StackTraceElement linha = pilha[i];
            saida.append(i == 0 ? "" : ",")
                    .append(texto(linha.getClassName() + "." + linha.getMethodName() + ":" + linha.getLineNumber()));
        }
        saida.append(']');
    }

    private static String versao(String valor) {
        return valor != null && valor.matches(VERSAO) ? valor : "unknown";
    }

    private static String nivel(Level nivel) {
        int valor = nivel.intValue();
        if (valor >= Level.SEVERE.intValue()) {
            return "ERROR";
        }
        if (valor >= Level.WARNING.intValue()) {
            return "WARN";
        }
        return valor >= Level.INFO.intValue() ? "INFO" : "DEBUG";
    }

    // Nomes de logger vêm do código (classes), mas uma biblioteca poderia usar qualquer texto.
    private static String nomeDoLogger(String nome) {
        return nome != null && nome.matches("[A-Za-z0-9_.$]{1,200}") ? nome : "other";
    }

    static String texto(String valor) {
        StringBuilder saida = new StringBuilder("\"");
        for (char c : valor.toCharArray()) {
            switch (c) {
                case '"' -> saida.append("\\\"");
                case '\\' -> saida.append("\\\\");
                default -> {
                    if (c < 32 || c == 127 || c == ' ' || c == ' ') {
                        saida.append(String.format("\\u%04x", (int) c));
                    } else {
                        saida.append(c);
                    }
                }
            }
        }
        return saida.append('"').toString();
    }
}

package br.com.inpetu.hub.comum.log;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Causa real de uma falha só com dados do código: nomes de classe, uma categoria fixa e o SQLState/código do banco.
 * Mensagens nunca são lidas, porque trazem SQL com valores, caminhos com o nome do usuário e dados de formulário.
 */
public record FalhaCausa(String categoria, List<Throwable> cadeia, String sqlState, Integer codigoSql) {

    static final int MAX_CAUSAS = 8;
    // H2: "Database may be already in use" (ErrorCode.DATABASE_ALREADY_OPEN_1).
    private static final int H2_BANCO_EM_USO = 90020;

    public static FalhaCausa de(Throwable falha) {
        List<Throwable> cadeia = cadeia(falha);
        List<String> nomes = cadeia.stream().map(t -> t.getClass().getName()).toList();
        SQLException sql = null;
        for (Throwable t : cadeia) {
            if (t instanceof SQLException s) {
                sql = s;
                break;
            }
        }
        String estado = sql != null && sql.getSQLState() != null && sql.getSQLState().matches("[0-9A-Z]{5}")
                ? sql.getSQLState()
                : null;
        Integer codigo = sql != null && sql.getErrorCode() >= 0 && sql.getErrorCode() <= 1_000_000_000
                ? sql.getErrorCode()
                : null;
        return new FalhaCausa(categoria(nomes, sql, codigo), cadeia, estado, codigo);
    }

    private static String categoria(List<String> nomes, SQLException sql, Integer codigo) {
        if (nomes.stream().anyMatch(n -> n.equals("java.net.BindException"))) {
            return "PORT_IN_USE";
        }
        if (codigo != null && codigo == H2_BANCO_EM_USO) {
            return "DATABASE_IN_USE";
        }
        if (nomes.stream().anyMatch(n -> n.startsWith("org.flywaydb."))) {
            return "MIGRATION_FAILED";
        }
        if (sql != null) {
            return "DATABASE_ERROR";
        }
        if (nomes.stream().anyMatch(n -> n.startsWith("java.nio.file.") && n.endsWith("Exception"))) {
            return "FILE_ACCESS";
        }
        if (nomes.contains("java.lang.OutOfMemoryError")) {
            return "OUT_OF_MEMORY";
        }
        return "OTHER";
    }

    /** A exceção e suas causas, sem repetir (cadeias circulares) e com limite. */
    private static List<Throwable> cadeia(Throwable topo) {
        List<Throwable> lista = new ArrayList<>();
        for (Throwable t = topo; t != null && lista.size() <= MAX_CAUSAS; t = t.getCause()) {
            if (lista.contains(t)) {
                break;
            }
            lista.add(t);
        }
        return List.copyOf(lista);
    }
}

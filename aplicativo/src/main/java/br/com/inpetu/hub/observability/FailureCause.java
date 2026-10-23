package br.com.inpetu.hub.observability;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import java.util.*;

/**
 * Causa real de uma falha (ex.: o sistema que não inicia) só com dados do código: nomes de classe,
 * linhas, uma categoria fixa e o SQLState/código numérico do banco. Mensagens nunca são lidas, porque
 * trazem SQL com valores, caminhos com o nome do usuário, URLs de banco e dados de formulário.
 */
public record FailureCause(String failure, List<String> causes, String sqlState, Integer sqlErrorCode) {
  static final int MAX_CAUSES = 8;
  // H2: "Database may be already in use" (ErrorCode.DATABASE_ALREADY_OPEN_1).
  private static final int H2_DATABASE_IN_USE = 90020;

  public static FailureCause of(Throwable failure) {
    return of(new ThrowableProxy(failure));
  }

  static FailureCause of(IThrowableProxy top) {
    List<IThrowableProxy> chain = chain(top);
    List<String> names = chain.stream().map(IThrowableProxy::getClassName).toList();
    java.sql.SQLException sql = null;
    for (IThrowableProxy p : chain)
      if (p instanceof ThrowableProxy tp && tp.getThrowable() instanceof java.sql.SQLException s) {
        sql = s;
        break;
      }
    String state = sql != null && sql.getSQLState() != null && sql.getSQLState().matches("[0-9A-Z]{5}") ? sql.getSQLState() : null;
    // Mesmo intervalo que o iniciador e o receptor aceitam (0 a 1e9): fora dele o código seria descartado lá.
    Integer code = sql != null && sql.getErrorCode() >= 0 && sql.getErrorCode() <= 1_000_000_000 ? sql.getErrorCode() : null;
    String failure;
    if (names.stream().anyMatch(n -> n.equals("java.net.BindException") || n.endsWith(".PortInUseException")))
      failure = "PORT_IN_USE";
    else if (code != null && code == H2_DATABASE_IN_USE) failure = "DATABASE_IN_USE";
    else if (names.stream().anyMatch(n -> n.startsWith("org.flywaydb."))) failure = "MIGRATION_FAILED";
    else if (sql != null || names.stream().anyMatch(n -> n.endsWith("SQLException"))) failure = "DATABASE_ERROR";
    else if (names.stream().anyMatch(n -> n.startsWith("java.nio.file.") && n.endsWith("Exception")))
      failure = "FILE_ACCESS";
    else if (names.contains("java.lang.OutOfMemoryError")) failure = "OUT_OF_MEMORY";
    else failure = "OTHER";
    return new FailureCause(failure, names.subList(1, names.size()), state, code);
  }

  /** A exceção e suas causas, sem repetir (cadeias circulares) e com limite. */
  static List<IThrowableProxy> chain(IThrowableProxy top) {
    List<IThrowableProxy> out = new ArrayList<>();
    for (IThrowableProxy c = top; c != null && out.size() <= MAX_CAUSES; c = c.getCause()) {
      if (out.contains(c)) break;
      out.add(c);
    }
    return out;
  }
}

package br.com.inpetu.hub.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Somente campos controlados: nunca serializa URI livre, formulário, login, senha ou mensagem de exceção.
 */
public final class OperationalEvents {
  private static final Logger LOG = LoggerFactory.getLogger(OperationalEvents.class);

  private OperationalEvents() {}

  public static void lifecycle(boolean started) {
    LOG.info("event={}", started ? "SERVER_READY" : "SERVER_STOPPED");
  }

  public static void retentionFailure(Throwable failure) {
    LOG.error("event=LOG_RETENTION_FAILED", failure);
  }

  public static void request(String method, int status, long millis, Throwable failure) {
    request(method, "OTHER", status, millis, failure);
  }

  public static void request(String method, String path, int status, long millis, Throwable failure) {
    String safeMethod =
        switch (method) {
          case "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS" -> method;
          default -> "OTHER";
        };
    String event =
        "event=HTTP method=" + safeMethod + " route=" + SafeRoutes.canonical(path)
            + " status=" + status + " durationMs=" + millis;
    if (failure != null) LOG.error(event, failure);
    else if (status >= 500) LOG.error(event);
    else if (status >= 400 || millis >= 1000) LOG.warn(event);
    else LOG.info(event);
  }

  public static void problem(String method, String path, int status, Throwable failure) {
    String safeMethod = switch (method) {
      case "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS" -> method;
      default -> "OTHER";
    };
    String event = "event=" + (status >= 500 ? "REQUEST_FAILED" : "REQUEST_REJECTED")
        + " method=" + safeMethod + " route=" + SafeRoutes.canonical(path) + " status=" + status;
    if (status >= 500) LOG.error(event, failure);
    else LOG.warn(event, failure);
  }

  public static void validation(String path, boolean conversion) {
    LOG.warn("event=VALIDATION_REJECTED route={} phase={} result=NEGADO",
        SafeRoutes.canonical(path), conversion ? "CONVERSION" : "FORM");
  }

  public static void mail(String phase, Throwable failure) {
    if (!java.util.Set.of("CONFIRMATION", "RESERVATION").contains(phase)) phase = "OTHER";
    if (failure != null) LOG.error("event=EMAIL_FAILED phase={}", phase, failure);
    else LOG.info("event=EMAIL_SENT phase={}", phase);
  }

  public static void mailNotConfigured() {
    LOG.info("event=EMAIL_NOT_CONFIGURED");
  }

  public static void audit(Long actor, String event, String target, Long targetId, String result) {
    // Códigos vêm do código-fonte; recusar dados livres mesmo se um chamador futuro errar.
    if (event == null || !event.matches("[A-Z_]{1,64}")) event = "OTHER";
    if (target == null || !target.matches("[a-z_]{1,40}")) target = "other";
    if (!"OK".equals(result) && !"NEGADO".equals(result)) result = "OTHER";
    LOG.info(
        "event=AUDIT actorId={} action={} target={} targetId={} result={}",
        actor,
        event,
        target,
        targetId,
        result);
  }
}

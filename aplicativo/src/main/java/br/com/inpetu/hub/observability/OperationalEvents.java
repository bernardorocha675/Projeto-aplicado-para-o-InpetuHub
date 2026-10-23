package br.com.inpetu.hub.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Somente campos controlados: nunca serializa URI livre, formulário, login, senha ou mensagem de exceção.
 */
public final class OperationalEvents {
  private static final Logger LOG = LoggerFactory.getLogger(OperationalEvents.class);

  private OperationalEvents() {}

  /** Marca a requisição respondida com 503 porque o servidor ainda está iniciando (AbuseFilter). */
  public static final String STARTING_ATTRIBUTE = "inpetu.startupWait";

  // O iniciador consulta /login até o sistema ficar pronto: cada 503 da espera não é um erro.
  private static final java.util.concurrent.atomic.AtomicInteger startupWaits =
      new java.util.concurrent.atomic.AtomicInteger();

  public static void lifecycle(boolean started) {
    if (started) LOG.info("event=SERVER_READY startupWaits={}", startupWaits.getAndSet(0));
    else LOG.info("event=SERVER_STOPPED");
  }

  /** Um aviso na primeira espera; a contagem sai no SERVER_READY. */
  public static void startupWait() {
    if (startupWaits.getAndIncrement() == 0) LOG.warn("event=SERVER_STARTING_WAIT");
  }

  /** O sistema não iniciou: a categoria vem de FailureCause e a cadeia de causas vai pelo encoder. */
  public static void startFailed(Throwable failure) {
    LOG.error("event=SERVER_START_FAILED failure={}", FailureCause.of(failure).failure(), failure);
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

  // Sem o termo pesquisado nem a mensagem do erro: só o tipo da falha.
  public static void citySearchUnavailable(Throwable failure) {
    LOG.warn("event=CITY_SEARCH_UNAVAILABLE exceptionType={}", failure.getClass().getName());
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

package br.com.inpetu.hub.observability;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.encoder.EncoderBase;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** JSON por linha. Mensagens livres de bibliotecas e de exceções nunca são serializadas. */
public class SafeLogEncoder extends EncoderBase<ILoggingEvent> {
  private static String version(String value) {
    return value != null && value.matches("0\\.(4|5)\\.(0|[1-9][0-9]*)") ? value : "unknown";
  }
  @Override
  public byte[] headerBytes() {
    return null;
  }

  @Override
  public byte[] footerBytes() {
    return null;
  }

  static String quote(String value) {
    StringBuilder out = new StringBuilder("\"");
    for (char c : value.toCharArray()) {
      switch (c) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        default -> {
          if (c < 32 || c == 127) out.append(String.format("\\u%04x", (int) c));
          else out.append(c);
        }
      }
    }
    return out.append('"').toString();
  }

  @Override
  public byte[] encode(ILoggingEvent e) {
    String id = e.getMDCPropertyMap().get("requestId");
    if (id == null || !id.matches("[a-f0-9-]{36}")) id = "-";
    String logger = e.getLoggerName();
    // A aplicação usa esta classe final para emitir apenas eventos de campos controlados.
    String message =
        OperationalEvents.class.getName().equals(logger)
            ? e.getFormattedMessage()
            : "FRAMEWORK_EVENT";
    StringBuilder out =
        new StringBuilder("{\"time\":")
            .append(quote(Instant.ofEpochMilli(e.getTimeStamp()).toString()))
            .append(",\"appVersion\":")
            .append(quote(version(System.getProperty("inpetu.app.version",
                SafeLogEncoder.class.getPackage().getImplementationVersion()))))
            .append(",\"launcherVersion\":")
            .append(quote(version(System.getProperty("inpetu.launcher.version"))))
            .append(",\"sessionId\":")
            .append(quote(System.getProperty("inpetu.session.id", "").matches("[a-f0-9]{32}")
                ? System.getProperty("inpetu.session.id") : "standalone"))
            .append(",\"level\":")
            .append(quote(e.getLevel().toString()))
            .append(",\"logger\":")
            .append(quote(logger))
            .append(",\"requestId\":")
            .append(quote(id))
            .append(",\"message\":")
            .append(quote(message));
    IThrowableProxy t = e.getThrowableProxy();
    if (t != null) {
      out.append(",\"exceptionType\":").append(quote(t.getClassName()));
      appendFrames(out, "frames", t, 20);
      // A causa real (o sistema que não inicia esconde-a atrás de BeanCreationException): só nomes de
      // classe, linhas do código, uma categoria e o SQLState; as mensagens da cadeia continuam fora.
      var chain = FailureCause.chain(t);
      var cause = FailureCause.of(t);
      if (chain.size() > 1) {
        out.append(",\"causes\":[");
        for (int i = 1; i < chain.size(); i++) {
          if (i != 1) out.append(',');
          out.append(quote(chain.get(i).getClassName()));
        }
        out.append(']');
        appendFrames(out, "rootFrames", chain.get(chain.size() - 1), 10);
      }
      out.append(",\"failure\":").append(quote(cause.failure()));
      if (cause.sqlState() != null) out.append(",\"sqlState\":").append(quote(cause.sqlState()));
      if (cause.sqlErrorCode() != null) out.append(",\"sqlErrorCode\":").append(cause.sqlErrorCode());
    }
    return out.append("}\n").toString().getBytes(StandardCharsets.UTF_8);
  }

  private static void appendFrames(StringBuilder out, String name, IThrowableProxy t, int max) {
    out.append(",\"").append(name).append("\":[");
    var frames = t.getStackTraceElementProxyArray();
    if (frames != null)
      for (int i = 0; i < Math.min(frames.length, max); i++) {
        if (i != 0) out.append(',');
        var f = frames[i].getStackTraceElement();
        out.append(quote(f.getClassName() + "." + f.getMethodName() + ":" + f.getLineNumber()));
      }
    out.append(']');
  }
}

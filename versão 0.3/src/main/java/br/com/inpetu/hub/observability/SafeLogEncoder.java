package br.com.inpetu.hub.observability;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.encoder.EncoderBase;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** JSON por linha. Mensagens livres de bibliotecas e de exceções nunca são serializadas. */
public class SafeLogEncoder extends EncoderBase<ILoggingEvent> {
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
      out.append(",\"frames\":[");
      var frames = t.getStackTraceElementProxyArray();
      if (frames != null)
        for (int i = 0; i < Math.min(frames.length, 20); i++) {
          if (i != 0) out.append(',');
          var f = frames[i].getStackTraceElement();
          out.append(quote(f.getClassName() + "." + f.getMethodName() + ":" + f.getLineNumber()));
        }
      out.append(']');
    }
    return out.append("}\n").toString().getBytes(StandardCharsets.UTF_8);
  }
}

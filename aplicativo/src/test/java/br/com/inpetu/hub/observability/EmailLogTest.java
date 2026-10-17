package br.com.inpetu.hub.observability;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.inpetu.hub.service.EmailService;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class EmailLogTest {
 @Test void smtpFailureIsDiagnosableWithoutRecipientCodeOrCredentials() {
  var sender=mock(JavaMailSender.class);
  when(sender.createMimeMessage()).thenThrow(new MailSendException("private-smtp-password recipient@example.com"));
  var service=new EmailService(sender,"sender@example.com","synthetic-smtp-password");
  var logger=(Logger)LoggerFactory.getLogger(OperationalEvents.class);
  var capture=new ListAppender<ILoggingEvent>();capture.start();logger.addAppender(capture);
  TransactionSynchronizationManager.initSynchronization();
  try {
   service.confirmacaoDepoisCommit("recipient@example.com","12345678");
   verifyNoInteractions(sender);
   TransactionSynchronizationManager.getSynchronizations().forEach(s->s.afterCommit());
   verify(sender).createMimeMessage();
   String log=capture.list.stream().map(e->new String(new SafeLogEncoder().encode(e),StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining());
   assertTrue(log.contains("EMAIL_FAILED"));assertTrue(log.contains("CONFIRMATION"));assertTrue(log.contains("MailSendException"));
   for(String secret:new String[]{"private-smtp-password","recipient@example.com","sender@example.com","12345678","synthetic-smtp-password"})assertFalse(log.contains(secret));
  } finally {TransactionSynchronizationManager.clearSynchronization();logger.detachAppender(capture);capture.stop();}
 }
 @Test void disabledMailDoesNotTrySmtpAndLogsConfigurationState() {
  var sender=mock(JavaMailSender.class);var service=new EmailService(sender,"","");
  var logger=(Logger)LoggerFactory.getLogger(OperationalEvents.class);var capture=new ListAppender<ILoggingEvent>();capture.start();logger.addAppender(capture);
  try {service.confirmacaoDepoisCommit("recipient@example.com","12345678");verifyNoInteractions(sender);assertTrue(capture.list.toString().contains("EMAIL_NOT_CONFIGURED"));}
  finally {logger.detachAppender(capture);capture.stop();}
 }
}

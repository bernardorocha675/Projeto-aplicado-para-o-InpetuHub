package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;

import br.com.inpetu.hub.observability.OperationalEvents;
import br.com.inpetu.hub.security.AuditService;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
    properties = {
      "spring.config.import=",
      "spring.datasource.url=jdbc:h2:mem:audit_v03;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
    })
@ActiveProfiles("test")
@Import(TestAccounts.class)
class AuditLogIntegrationTest {
  @Autowired AuditService audit;
  @Autowired PlatformTransactionManager manager;

  @Test
  void auditMirrorOnlyAppearsAfterCommitAndNeverIncludesDetails() {
    var logger = (Logger) LoggerFactory.getLogger(OperationalEvents.class);
    var capture = new ListAppender<ILoggingEvent>();
    capture.start();
    logger.addAppender(capture);
    try {
      var tx = new TransactionTemplate(manager);
      tx.executeWithoutResult(
          status -> {
            audit.record(null, "LOG_TEST", "solicitacao", 123L, "OK", "private-detail");
            assertTrue(capture.list.isEmpty());
          });
      assertEquals(1, capture.list.size());
      assertTrue(capture.list.get(0).getFormattedMessage().contains("action=LOG_TEST"));
      assertFalse(capture.list.get(0).getFormattedMessage().contains("private-detail"));
      capture.list.clear();
      tx.executeWithoutResult(
          status -> {
            audit.record(null, "ROLLED_BACK", "solicitacao", 123L, "OK", null);
            status.setRollbackOnly();
          });
      assertTrue(capture.list.isEmpty());
      tx.executeWithoutResult(
          status -> {
            audit.security(null, "LOGIN_NEGADO", "NEGADO");
            status.setRollbackOnly();
          });
      assertEquals(1, capture.list.size());
      assertTrue(capture.list.get(0).getFormattedMessage().contains("LOGIN_NEGADO"));
    } finally {
      logger.detachAppender(capture);
      capture.stop();
    }
  }
}

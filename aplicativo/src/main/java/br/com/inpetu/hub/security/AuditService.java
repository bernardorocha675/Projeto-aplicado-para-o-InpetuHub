package br.com.inpetu.hub.security;

import br.com.inpetu.hub.entity.Auditoria;
import br.com.inpetu.hub.repository.AuditoriaRepository;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class AuditService {
  private final AuditoriaRepository repo;
  private final Clock clock;

  public AuditService(AuditoriaRepository repo, Clock clock) {
    this.repo = repo;
    this.clock = clock;
  }

  // Eventos de negócio fazem parte da mesma transação; não registrar senha ou conteúdo de
  // formulários.
  @Transactional
  public void record(
      Long actor, String event, String target, Long targetId, String result, String details) {
    Auditoria a = new Auditoria();
    a.instante = LocalDateTime.now(clock);
    a.autorId = actor;
    a.evento = event;
    a.alvo = target;
    a.alvoId = targetId;
    a.resultado = result;
    a.detalhes = details;
    repo.save(a);
    // Só espelhar operações confirmadas; nunca copiar o campo livre de detalhes.
    org.springframework.transaction.support.TransactionSynchronizationManager
        .registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
              @Override
              public void afterCommit() {
                br.com.inpetu.hub.observability.OperationalEvents.audit(
                    actor, event, target, targetId, result);
              }
            });
  }

  // Tentativas negadas devem sobreviver ao rollback de outra operação.
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void security(Long actor, String event, String result) {
    record(actor, event, "autenticacao", actor, result, null);
  }
}

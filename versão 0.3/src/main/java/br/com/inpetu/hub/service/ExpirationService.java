package br.com.inpetu.hub.service;

import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.enums.StatusReserva;
import br.com.inpetu.hub.repository.*;
import br.com.inpetu.hub.security.AuditService;
import jakarta.persistence.EntityManager;
import java.time.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@EnableScheduling
public class ExpirationService {
  private final ConfiguracaoRepository config;
  private final EntityManager em;
  private final Clock clock;
  private final AuditService audit;

  public ExpirationService(ConfiguracaoRepository c, EntityManager e, Clock cl, AuditService a) {
    config = c;
    em = e;
    clock = cl;
    audit = a;
  }

  @Scheduled(fixedDelay = 60000, initialDelay = 60000)
  @Transactional
  public void expire() {
    config.bloquear();
    var now = LocalDateTime.now(clock);
    for (var r :
        em.createQuery("from Reserva r where r.statusReserva=:status", Reserva.class)
            .setParameter("status", StatusReserva.PENDENTE)
            .getResultList()) {
      if (r.expiraEm == null) r.expiraEm = now.plusHours(24);
      else if (!r.expiraEm.isAfter(now)) {
        r.statusReserva = StatusReserva.CANCELADA;
        audit.record(null, "PENDENTE_EXPIRADA", "reservas", r.id, "OK", null);
      }
    }
    for (var s :
        em.createQuery(
                "from SolicitacaoPublica s where s.estado='RECEBIDA' and s.expiraEm<=:now",
                SolicitacaoPublica.class)
            .setParameter("now", now)
            .getResultList()) {
      s.estado = "EXPIRADA";
      s.codigoHash = null;
      audit.record(null, "SOLICITACAO_EXPIRADA", "solicitacao", s.id, "OK", null);
    }
  }
}

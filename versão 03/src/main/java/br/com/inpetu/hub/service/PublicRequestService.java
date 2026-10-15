package br.com.inpetu.hub.service;

import br.com.inpetu.hub.dto.ReservaForm;
import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.exception.RegraException;
import br.com.inpetu.hub.repository.*;
import br.com.inpetu.hub.security.*;
import jakarta.persistence.*;
import java.security.SecureRandom;
import java.time.*;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicRequestService {
  private final SolicitacaoPublicaRepository repo;
  private final ConfiguracaoRepository config;
  private final EmpreendedorRepository people;
  private final ReservaService reservations;
  private final EmailService mail;
  private final SecretCrypto crypto;
  private final AuditService audit;
  private final Clock clock;
  private final boolean emailRequired;
  private final EntityManager em;

  public PublicRequestService(
      SolicitacaoPublicaRepository r,
      ConfiguracaoRepository c,
      EmpreendedorRepository p,
      ReservaService b,
      EmailService m,
      SecretCrypto s,
      AuditService a,
      Clock cl,
      @Value("${app.security.public-email-required:false}") boolean required,
      EntityManager em) {
    repo = r;
    config = c;
    people = p;
    reservations = b;
    mail = m;
    crypto = s;
    audit = a;
    clock = cl;
    emailRequired = required;
    this.em = em;
  }

  @Transactional
  public SolicitacaoPublica submit(ReservaForm form, String key) {
    if (key == null || !key.matches("[A-Za-z0-9-]{16,64}"))
      throw new RegraException("Reabra o formulário antes de confirmar.");
    config.bloquear();
    var old = repo.findByChave(key);
    if (old.isPresent()) return old.get();
    reservations.estimar(form); // validar horários e dados sem reservar nem vincular ao CRM.
    SolicitacaoPublica s = new SolicitacaoPublica();
    s.form = form;
    s.chave = key;
    s.referencia = SecretCrypto.random(32);
    s.criadaEm = LocalDateTime.now(clock);
    s.expiraEm = s.criadaEm.plusHours(24);
    s.codigoExpiraEm = s.criadaEm.plusMinutes(10);
    s.estado = "RECEBIDA";
    String destination = people.findByCnpj(form.cnpj).map(p -> p.email).orElse(form.email);
    if (emailRequired && !mail.configurado())
      throw new RegraException("Envio de confirmação indisponível. Entre em contato com a equipe.");
    if (mail.configurado()) {
      String code = String.format(Locale.ROOT, "%08d", new SecureRandom().nextInt(100000000));
      s.codigoHash = crypto.fingerprint("public:" + s.referencia + ":" + code);
      mail.confirmacaoDepoisCommit(destination, code);
    }
    repo.saveAndFlush(s);
    audit.record(
        null,
        "SOLICITACAO_RECEBIDA",
        "solicitacao",
        s.id,
        "OK",
        "Sem bloqueio de horário e sem alocação de cotas.");
    return s;
  }

  @Transactional
  public boolean confirm(String reference, String code) {
    config.bloquear();
    var found = repo.findByReferencia(reference);
    if (found.isEmpty()) return false;
    var s = found.get();
    if (!s.estado.equals("RECEBIDA")
        || s.emailConfirmado
        || s.codigoHash == null
        || s.tentativas >= 5
        || s.codigoExpiraEm.isBefore(LocalDateTime.now(clock))) return false;
    s.tentativas++;
    if (code == null
        || !code.matches("[0-9]{8}")
        || !crypto.matches(s.codigoHash, crypto.fingerprint("public:" + s.referencia + ":" + code)))
      return false;
    s.emailConfirmado = true;
    s.codigoHash = null;
    audit.record(null, "EMAIL_CONFIRMADO", "solicitacao", s.id, "OK", null);
    return true;
  }

  @Transactional
  public Long accept(Long id, Colaborador actor, boolean identity, String reason) {
    config.bloquear();
    var s = em.find(SolicitacaoPublica.class, id, LockModeType.PESSIMISTIC_WRITE);
    if (s == null) throw new RegraException("Solicitação não encontrada.");
    if (!s.estado.equals("RECEBIDA") || s.expiraEm.isBefore(LocalDateTime.now(clock)))
      throw new RegraException("Solicitação processada ou expirada.");
    if (!identity || reason == null || reason.isBlank() || reason.length() > 2000)
      throw new RegraException(
          "Confira a identidade e informe como o vínculo com a instituição foi verificado.");
    if (emailRequired && !s.emailConfirmado)
      throw new RegraException("A confirmação de email ainda não foi concluída.");
    var r = reservations.solicitar(s.form);
    s.reservaId = r.id;
    s.estado = "ENCAMINHADA";
    s.justificativa = reason;
    s.codigoHash = null;
    audit.record(
        actor.id,
        "IDENTIDADE_CONFERIDA",
        "solicitacao",
        s.id,
        "OK",
        "emailConfirmado=" + s.emailConfirmado);
    return r.id;
  }

  @Transactional
  public void reject(Long id, Colaborador actor) {
    config.bloquear();
    var s = repo.findById(id).orElseThrow(() -> new RegraException("Solicitação não encontrada."));
    if (!s.estado.equals("RECEBIDA")) throw new RegraException("Solicitação já processada.");
    s.estado = "RECUSADA";
    s.codigoHash = null;
    audit.record(actor.id, "SOLICITACAO_RECUSADA", "solicitacao", s.id, "OK", null);
  }
}

package br.com.inpetu.hub.security;

import br.com.inpetu.hub.entity.Colaborador;
import br.com.inpetu.hub.exception.RegraException;
import br.com.inpetu.hub.repository.ColaboradorRepository;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MfaService {
  private final ColaboradorRepository users;
  private final TotpService totp;
  private final SecretCrypto crypto;
  private final Clock clock;
  private final AuditService audit;
  private final RateLimits limits;

  public MfaService(
      ColaboradorRepository users,
      TotpService totp,
      SecretCrypto crypto,
      Clock clock,
      AuditService audit,
      RateLimits limits) {
    this.limits = limits;
    this.users = users;
    this.totp = totp;
    this.crypto = crypto;
    this.clock = clock;
    this.audit = audit;
  }

  @Transactional
  public List<String> enroll(Long id, String secret, String code) {
    if (!limits.allow("mfa:" + id, 8, java.time.Duration.ofMinutes(5)))
      throw new RegraException("Muitas tentativas de segundo fator. Aguarde cinco minutos.");
    Colaborador c = users.locked(id).orElseThrow();
    if (c.mfaSecret != null) throw new RegraException("Segundo fator já configurado.");
    long counter = totp.verify(secret, code, clock.instant().getEpochSecond(), -1);
    if (counter < 0) throw new RegraException("Código inválido ou expirado.");
    c.mfaSecret = crypto.encrypt(secret);
    c.mfaCounter = counter;
    c.securityVersion++;
    var recovery = new ArrayList<String>();
    for (int i = 0; i < 8; i++) recovery.add(SecretCrypto.random(18));
    c.mfaRecovery =
        String.join(",", recovery.stream().map(s -> crypto.fingerprint("recovery:" + s)).toList());
    audit.record(id, "MFA_ATIVADO", "colaboradores", id, "OK", null);
    return recovery;
  }

  @Transactional
  public boolean verify(Long id, String code) {
    if (!limits.allow("mfa:" + id, 8, java.time.Duration.ofMinutes(5)))
      throw new RegraException("Muitas tentativas de segundo fator. Aguarde cinco minutos.");
    Colaborador c = users.locked(id).orElseThrow();
    if (c.mfaSecret == null || code == null || code.length() > 128) return false;
    long counter =
        totp.verify(
            crypto.decrypt(c.mfaSecret), code, clock.instant().getEpochSecond(), c.mfaCounter);
    if (counter >= 0) {
      c.mfaCounter = counter;
      return true;
    }
    if (c.mfaRecovery == null) return false;
    String candidate = crypto.fingerprint("recovery:" + code);
    var remaining = new ArrayList<>(Arrays.asList(c.mfaRecovery.split(",")));
    boolean removed = remaining.removeIf(s -> crypto.matches(s, candidate));
    if (removed) {
      c.mfaRecovery = String.join(",", remaining);
      audit.record(id, "MFA_RECUPERACAO", "colaboradores", id, "OK", null);
    }
    return removed;
  }
}

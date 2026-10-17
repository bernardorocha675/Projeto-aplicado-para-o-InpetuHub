package br.com.inpetu.hub.service;

import br.com.inpetu.hub.entity.Reserva;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.*;
import org.springframework.web.util.HtmlUtils;

@Service
public class EmailService {

  private final JavaMailSender sender;
  private final String usuario, senha;

  public EmailService(
      JavaMailSender s,
      @Value("${spring.mail.username}") String u,
      @Value("${spring.mail.password}") String p) {
    sender = s;
    usuario = u;
    senha = p;
  }

  public boolean configurado() {
    return !usuario.isBlank() && !senha.isBlank();
  }

  public void confirmacaoDepoisCommit(String destino, String code) {
    if (!configurado()) {
      br.com.inpetu.hub.observability.OperationalEvents.mailNotConfigured();
      return;
    }
    org.springframework.transaction.support.TransactionSynchronizationManager
        .registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
              @Override
              public void afterCommit() {
                try {
                  var msg = sender.createMimeMessage();
                  var helper = new MimeMessageHelper(msg, "UTF-8");
                  helper.setFrom(usuario);
                  helper.setTo(destino);
                  helper.setSubject("Confirmação de solicitação InPETU");
                  helper.setText(
                      "Código: "
                          + code
                          + ". Válido por dez minutos no navegador da solicitação. Se você não"
                          + " solicitou, ignore esta mensagem.",
                      false);
                  sender.send(msg);
                  br.com.inpetu.hub.observability.OperationalEvents.mail("CONFIRMATION", null);
                } catch (Exception ex) {
                  br.com.inpetu.hub.observability.OperationalEvents.mail("CONFIRMATION", ex);
                }
              }
            });
  }

  public void enviarDepoisCommit(Reserva r, String acao) {
    String destino = r.empreendedor.email;
    String titulo = "InPETU Hub • " + acao;
    String body =
        "<div style='font-family:Arial;color:#203449;max-width:640px'><h1"
            + " style='color:#087fc5'>InPETUhub</h1><h2>"
            + HtmlUtils.htmlEscape(acao)
            + "</h2><p>Reserva #"
            + r.id
            + " — "
            + HtmlUtils.htmlEscape(r.titulo)
            + "</p><p>"
            + r.data
            + " • "
            + r.horaInicio
            + "–"
            + r.horaFim
            + " • "
            + HtmlUtils.htmlEscape(r.espaco.nomeEspaco)
            + "</p><p>Status: "
            + r.statusReserva
            + "</p><p>Valor estimado: R$ "
            + r.valorFinal
            + ". Sinal: "
            + r.percentualSinal
            + "% (R$ "
            + r.valorSinal
            + "). Restante: R$ "
            + r.valorRestante
            + ".</p>"
            + (r.motivoRecusa == null
                ? ""
                : "<p>Motivo: " + HtmlUtils.htmlEscape(r.motivoRecusa) + "</p>")
            + (!r.taxasConfiguradas
                ? "<p>Operação/limpeza ainda dependem de configuração. Valores preliminares.</p>"
                : "")
            + "</div>";
    Runnable task =
        () -> {
          if (usuario.isBlank() || senha.isBlank()) {
            br.com.inpetu.hub.observability.OperationalEvents.mailNotConfigured();
            return;
          }
          try {
            var msg = sender.createMimeMessage();
            var helper = new MimeMessageHelper(msg, "UTF-8");
            helper.setFrom(usuario);
            helper.setTo(destino);
            helper.setSubject(titulo);
            helper.setText(body, true);
            sender.send(msg);
            br.com.inpetu.hub.observability.OperationalEvents.mail("RESERVATION", null);
          } catch (Exception ex) {
            br.com.inpetu.hub.observability.OperationalEvents.mail("RESERVATION", ex);
          }
        };
    if (TransactionSynchronizationManager.isSynchronizationActive())
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              task.run();
            }
          });
    else task.run();
  }
}

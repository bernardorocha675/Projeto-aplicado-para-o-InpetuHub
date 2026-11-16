package br.com.inpetu.hub.solicitacao;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jetty.util.component.AbstractLifeCycle;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.log.Eventos;
import br.com.inpetu.hub.reserva.ReservaServico;

/** A cada minuto, vence os pedidos e as reservas pendentes com mais de 24 h. Começa e para junto com o servidor. */
final class VencimentoDePedidos extends AbstractLifeCycle {

    private static final Logger LOG = Logger.getLogger(VencimentoDePedidos.class.getName());

    private final SolicitacaoServico servico;
    private final ReservaServico reservas;
    private ScheduledExecutorService agenda;

    VencimentoDePedidos(Ambiente ambiente) {
        this.servico = new SolicitacaoServico(ambiente);
        this.reservas = new ReservaServico(ambiente);
    }

    @Override
    protected void doStart() {
        agenda = Executors.newSingleThreadScheduledExecutor(tarefa -> {
            Thread linha = new Thread(tarefa, "inpetu-vencimento-pedidos");
            linha.setDaemon(true);
            return linha;
        });
        agenda.scheduleWithFixedDelay(this::vencer, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    protected void doStop() throws InterruptedException {
        agenda.shutdownNow();
        agenda.awaitTermination(5, TimeUnit.SECONDS);
    }

    private void vencer() {
        try {
            servico.expirarVencidas();
            reservas.expirarPendentes();
        } catch (RuntimeException falha) {
            // Uma falha (banco ocupado, por exemplo) não pode parar as próximas rodadas.
            Eventos.falhaEmTarefa("PEDIDOS_VENCIDOS", falha);
            LOG.log(Level.FINE, "rodada de vencimento falhou", falha);
        }
    }
}

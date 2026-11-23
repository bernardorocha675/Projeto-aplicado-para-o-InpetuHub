package br.com.inpetu.hub.interna;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.AmbienteDeTeste;
import br.com.inpetu.hub.comum.RegraException;
import br.com.inpetu.hub.conta.Contas;
import br.com.inpetu.hub.conta.Perfil;
import br.com.inpetu.hub.reserva.ReservaServico;
import br.com.inpetu.hub.solicitacao.SolicitacaoServico;

/** Porta do ConcorrenciaIntegrationTest: aprovações ao mesmo tempo não ocupam o mesmo horário nem a mesma cota. */
@Timeout(60)
class ConcorrenciaTest {

    private List<String> simultaneas(Ambiente ambiente, List<Long> reservas, long admin) throws Exception {
        ReservaServico servico = new ReservaServico(ambiente);
        ExecutorService grupo = Executors.newFixedThreadPool(reservas.size());
        CountDownLatch largada = new CountDownLatch(1);
        try {
            List<Future<String>> futuros = new ArrayList<>();
            for (long reserva : reservas) {
                futuros.add(grupo.submit(() -> {
                    largada.await();
                    try {
                        servico.aprovar(reserva, admin);
                        return "true".equals(Contas.texto(ambiente.banco(),
                                "select lower(cast(usa_cota as varchar)) from reserva where id_reserva = ?", reserva))
                                        ? "COTA"
                                        : "SEM_COTA";
                    } catch (RegraException conflito) {
                        return "CONFLITO";
                    }
                }));
            }
            largada.countDown();
            List<String> resultados = new ArrayList<>();
            for (Future<String> futuro : futuros) {
                resultados.add(futuro.get(30, TimeUnit.SECONDS));
            }
            return resultados;
        } finally {
            grupo.shutdownNow();
        }
    }

    private static long aceitar(Ambiente ambiente, Map<String, String> campos, long colaborador) throws Exception {
        return new SolicitacaoServico(ambiente).aceitar(Cenario.pedido(ambiente, campos), colaborador, true, "ok");
    }

    @Test
    void soUmaAprovacaoOcupaOMesmoIntervalo() throws Exception {
        for (int rodada = 0; rodada < 5; rodada++) {
            Ambiente ambiente = AmbienteDeTeste.novo();
            Cenario.taxasZeradas(ambiente);
            long admin = Contas.criar(ambiente.banco(), Perfil.ADMIN).id();
            long a = aceitar(ambiente, Cenario.campos(ambiente), admin);
            long b = aceitar(ambiente, Cenario.campos(ambiente), admin);
            List<String> resultados = simultaneas(ambiente, List.of(a, b), admin);
            assertEquals(1, resultados.stream().filter("SEM_COTA"::equals).count(), resultados.toString());
            assertEquals(1, resultados.stream().filter("CONFLITO"::equals).count(), resultados.toString());
        }
    }

    @Test
    void cotaCompartilhadaNaoEUsadaDuasVezes() throws Exception {
        for (int rodada = 0; rodada < 5; rodada++) {
            Ambiente ambiente = AmbienteDeTeste.novo();
            Cenario.taxasZeradas(ambiente);
            Contas.executar(ambiente.banco(), "update persona set cota_mensal_horas = 6 where codigo = 'P3'");
            long admin = Contas.criar(ambiente.banco(), Perfil.ADMIN).id();
            Map<String, String> c = Cenario.campos(ambiente);
            c.put("personaId", String.valueOf(Cenario.persona(ambiente, "P3")));
            c.put("data", "2026-11-30");
            long a = aceitar(ambiente, c, admin);
            c.put("espacoId", String.valueOf(Cenario.espaco(ambiente, "Coworking")));
            long b = aceitar(ambiente, c, admin);
            List<String> resultados = simultaneas(ambiente, List.of(a, b), admin);
            assertEquals(1, resultados.stream().filter("COTA"::equals).count(), resultados.toString());
            assertEquals(1, resultados.stream().filter("SEM_COTA"::equals).count(), resultados.toString());
        }
    }
}

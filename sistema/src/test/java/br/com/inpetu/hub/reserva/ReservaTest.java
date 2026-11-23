package br.com.inpetu.hub.reserva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.inpetu.hub.comum.RegraException;

/**
 * O ciclo da reserva com o relógio fixo em 05/10/2026, 12h. Porta dos testes de regra do FluxosIntegrationTest e do
 * SecurityIntegrationTest (refusingOrCancellingUnallocatedPendingCannotCreateQuotaCredit).
 */
class ReservaTest {

    private final Cenario cenario = new Cenario();

    private static BigDecimal horas(String valor) {
        return new BigDecimal(valor);
    }

    private static void mesmoValor(String esperado, BigDecimal valor) {
        assertEquals(0, horas(esperado).compareTo(valor), "esperado " + esperado + ", veio " + valor);
    }

    @Test
    void pedidoNascePendenteSemOcuparCotaEExpiraEm24Horas() {
        Reserva r = cenario.solicitar(Cenario.pedido(Cenario.persona("P2"), LocalDate.of(2026, 10, 20)));
        assertEquals(SituacaoReserva.PENDENTE, r.situacao());
        assertEquals(SituacaoPagamento.PENDENTE, r.pagamento());
        assertEquals(Cenario.AGORA.plusHours(24), r.expiraEm());
        assertTrue(r.orcamento().usaCota());
        mesmoValor("0", cenario.cota(r.pedido().persona(), r.pedido().data()).utilizadas());
        mesmoValor("30", cenario.cota(r.pedido().persona(), r.pedido().data()).saldo());
    }

    @Test
    void aprovacaoUsaACotaERegistraOResponsavel() {
        Reserva r = cenario.solicitar(Cenario.pedido(Cenario.persona("P2"), LocalDate.of(2026, 10, 20)));
        Reserva.Efeito efeito = cenario.aprovar(r);
        assertEquals(SituacaoReserva.APROVADA, r.situacao());
        assertEquals(Cenario.ADMIN, r.responsavel());
        assertEquals("APROVAÇÃO", efeito.acao());
        mesmoValor("4", cenario.cota(r.pedido().persona(), r.pedido().data()).utilizadas());
        assertThrows(RegraException.class, () -> cenario.aprovar(r), "só pendente pode ser aprovada");
    }

    @Test
    void pendenteExpiradaNaoPodeSerAprovadaEAExpiracaoCancela() {
        Reserva r = cenario.solicitar(Cenario.pedido(Cenario.persona("P6"), LocalDate.of(2026, 10, 20)));
        assertFalse(r.expirar(Cenario.AGORA.plusHours(23)));
        assertEquals(SituacaoReserva.PENDENTE, r.situacao());
        assertEquals("Reserva pendente expirada.", assertThrows(RegraException.class, () -> r.aprovar(cenario
                .orcamento(r.pedido(), BigDecimal.ZERO), Cenario.ADMIN, Cenario.AGORA.plusHours(25))).getMessage());
        assertTrue(r.expirar(Cenario.AGORA.plusHours(24)));
        assertEquals(SituacaoReserva.CANCELADA, r.situacao());
        assertFalse(r.expirar(Cenario.AGORA.plusHours(48)), "só uma vez");
    }

    @Test
    void exigeTaxasConfiguradasParaAprovar() {
        cenario.configuracao = new Configuracao(4, LocalTime.of(8, 0), LocalTime.of(20, 0), BigDecimal.ZERO, null,
                BigDecimal.ZERO);
        Reserva r = cenario.solicitar(Cenario.pedido(Cenario.persona("P6"), LocalDate.of(2026, 10, 20)));
        assertEquals("Configure os valores de operação/limpeza na Administração antes de aprovar.", assertThrows(
                RegraException.class, () -> cenario.aprovar(r)).getMessage());
        assertEquals(SituacaoReserva.PENDENTE, r.situacao());
    }

    @Test
    void recusaExigeMotivoESoValeParaPendente() {
        Reserva r = cenario.solicitar(Cenario.pedido(Cenario.persona("P3"), LocalDate.of(2026, 10, 20)));
        assertThrows(RegraException.class, () -> r.recusar("", Cenario.ADMIN));
        assertThrows(RegraException.class, () -> r.recusar("x".repeat(2001), Cenario.ADMIN));
        Reserva.Efeito efeito = cenario.registrar(r.recusar("Documentação incompleta", Cenario.ADMIN));
        assertEquals(SituacaoReserva.RECUSADA, r.situacao());
        assertEquals("Documentação incompleta", r.motivoRecusa());
        assertEquals("RECUSA", efeito.acao());
        assertEquals(List.of(), efeito.movimentos());
        assertThrows(RegraException.class, () -> r.recusar("De novo", Cenario.ADMIN));
    }

    @Test
    void aprovacaoBloqueiaOHorarioMasPendentesNao() {
        Pedido pedido = Cenario.pedido(Cenario.persona("P6"), LocalDate.of(2026, 10, 20));
        Reserva primeira = cenario.solicitar(pedido);
        Reserva pendente = cenario.solicitar(pedido, List.of());
        cenario.aprovar(primeira);
        List<Intervalo> aprovadas = List.of(primeira.intervalo());
        assertThrows(RegraException.class, () -> Horarios.validar(Cenario.SALA, pendente.pedido().data(), pendente
                .pedido().inicio(), pendente.pedido().fim(), 10, cenario.configuracao, aprovadas, Cenario.RELOGIO),
                "a segunda não pode ser aprovada");
        Pedido sobreposto = new Pedido(pedido.persona(), 7L, pedido.data(), LocalTime.of(11, 0), LocalTime.of(14, 0),
                10, false, false, true, "T", "O", null);
        assertThrows(RegraException.class, () -> cenario.solicitar(sobreposto, aprovadas));
    }

    @Test
    void alterarParaODomingoRecalculaOSinal() {
        Reserva r = cenario.solicitar(Cenario.pedido(Cenario.persona("P6"), LocalDate.of(2026, 10, 20)));
        assertEquals(30, r.orcamento().percentualSinal());
        Pedido domingo = Cenario.pedido(Cenario.persona("P6"), LocalDate.of(2026, 10, 25));
        Reserva.Efeito efeito = cenario.registrar(r.alterar(domingo, cenario.orcamento(domingo, r.creditoDeCota()),
                Cenario.ADMIN));
        assertEquals(60, r.orcamento().percentualSinal());
        assertEquals(new BigDecimal("960.00"), r.orcamento().valorSinal());
        assertEquals("ALTERAÇÃO", efeito.acao());
        assertEquals("2026-10-20 08:00–12:00 / P6 → 2026-10-25 08:00–12:00 / P6", efeito.descricao());
    }

    @Test
    void alterarAprovadaDevolveACotaAntigaEUsaANova() {
        Reserva r = cenario.solicitar(Cenario.pedido(Cenario.persona("P3"), LocalDate.of(2026, 10, 20)));
        cenario.aprovar(r);
        Pedido novembro = Cenario.pedido(Cenario.persona("P3"), LocalDate.of(2026, 11, 10));
        cenario.registrar(r.alterar(novembro, cenario.orcamento(novembro, r.creditoDeCota()), Cenario.ADMIN));
        mesmoValor("0", cenario.cota(Cenario.persona("P3"), LocalDate.of(2026, 10, 20)).utilizadas());
        mesmoValor("4", cenario.cota(Cenario.persona("P3"), LocalDate.of(2026, 11, 10)).utilizadas());
        assertEquals(SituacaoReserva.APROVADA, r.situacao());
    }

    @Test
    void comPagamentoRegistradoNaoAltera() {
        Reserva r = cenario.solicitar(Cenario.pedido(Cenario.persona("P6"), LocalDate.of(2026, 10, 20)));
        cenario.aprovar(r);
        r.registrarPagamento(SituacaoPagamento.SINAL_PAGO, Cenario.ADMIN);
        Pedido outro = Cenario.pedido(Cenario.persona("P6"), LocalDate.of(2026, 10, 21));
        assertThrows(RegraException.class, () -> r.alterar(outro, cenario.orcamento(outro, BigDecimal.ZERO),
                Cenario.ADMIN));
    }

    @Test
    void cancelamentoComMenosDe30DiasDobraAsHorasEPassaADividaAdiante() {
        Persona p4 = new Persona(4, "P4", horas("6"));
        Reserva r = cenario.solicitar(Cenario.pedido(p4, LocalDate.of(2026, 10, 20)));
        cenario.aprovar(r);
        Reserva.Efeito efeito = cenario.registrar(r.cancelar(false, "Desistência", false, Cenario.ADMIN,
                Cenario.AGORA));
        assertEquals(SituacaoReserva.CANCELADA, r.situacao());
        assertEquals("Penalidade: 8.00h. Desistência", efeito.descricao());
        CotaDoMes outubro = cenario.cota(p4, r.pedido().data());
        mesmoValor("8", outubro.utilizadas());
        mesmoValor("2", outubro.saldoDevedor());
        mesmoValor("4", cenario.cota(p4, LocalDate.of(2026, 11, 1)).saldo());
    }

    @Test
    void cancelamentoAntecipadoNaoPenaliza() {
        Reserva r = cenario.solicitar(Cenario.pedido(Cenario.persona("P3"), LocalDate.of(2026, 11, 10)));
        cenario.aprovar(r);
        Reserva.Efeito efeito = cenario.registrar(r.cancelar(false, "Antecipado", false, Cenario.ADMIN,
                Cenario.AGORA));
        assertEquals("Sem penalidade. Antecipado", efeito.descricao());
        mesmoValor("0", cenario.cota(Cenario.persona("P3"), r.pedido().data()).utilizadas());
    }

    @Test
    void eventoAprovadoJaTerminadoSoSaiPorForcaMaiorDoGestor() {
        Persona p4 = new Persona(4, "P4", horas("6"));
        Pedido hojeDeManha = Cenario.pedido(p4, LocalDate.of(2026, 10, 5));
        // Aprovada antes (com cota); o relógio já passou das 12h, fim do evento.
        Reserva r = Reserva.solicitar(hojeDeManha, cenario.orcamento(Cenario.pedido(p4, LocalDate.of(2026, 10, 20)),
                BigDecimal.ZERO), Cenario.AGORA.minusDays(20));
        cenario.registrar(r.aprovar(cenario.orcamento(Cenario.pedido(p4, LocalDate.of(2026, 10, 20)),
                BigDecimal.ZERO), Cenario.ADMIN, Cenario.AGORA.minusDays(20)));
        BigDecimal antes = cenario.cota(p4, hojeDeManha.data()).utilizadas();
        RegraException erro = assertThrows(RegraException.class, () -> r.cancelar(false, "Tarde", false,
                Cenario.ADMIN, Cenario.AGORA));
        assertTrue(erro.getMessage().contains("já terminou"), erro.getMessage());
        assertEquals(SituacaoReserva.APROVADA, r.situacao());
        assertEquals(0, antes.compareTo(cenario.cota(p4, hojeDeManha.data()).utilizadas()));
        assertEquals("A exceção exige um gestor e justificativa.", assertThrows(RegraException.class, () -> r
                .cancelar(true, "Interdição", false, Cenario.ADMIN, Cenario.AGORA)).getMessage());
        assertThrows(RegraException.class, () -> r.cancelar(true, " ", true, Cenario.ADMIN, Cenario.AGORA));
        cenario.registrar(r.cancelar(true, "Interdição registrada pelo gestor", true, Cenario.ADMIN, Cenario.AGORA));
        assertEquals(SituacaoReserva.CANCELADA, r.situacao());
        mesmoValor("0", cenario.cota(p4, hojeDeManha.data()).utilizadas());
    }

    @Test
    void eventoAprovadoAindaNaoTerminadoContinuaCancelavelComPenalidade() {
        Persona p4 = new Persona(4, "P4", horas("6"));
        Pedido hojeAteAs13 = new Pedido(p4, 7L, LocalDate.of(2026, 10, 5), LocalTime.of(8, 0), LocalTime.of(13, 0), 10,
                false, false, true, "T", "O", null);
        Orcamento comCota = cenario.orcamento(Cenario.pedido(p4, LocalDate.of(2026, 10, 20)), BigDecimal.ZERO);
        Reserva r = Reserva.solicitar(hojeAteAs13, comCota, Cenario.AGORA.minusDays(20));
        cenario.registrar(r.aprovar(comCota, Cenario.ADMIN, Cenario.AGORA.minusDays(20)));
        cenario.registrar(r.cancelar(false, "Desistência no dia", false, Cenario.ADMIN, Cenario.AGORA));
        assertEquals(SituacaoReserva.CANCELADA, r.situacao());
        mesmoValor("8", cenario.cota(p4, hojeAteAs13.data()).utilizadas());
    }

    @Test
    void cancelarOuRecusarPendenteNaoMexeNaCota() {
        Reserva r = cenario.solicitar(Cenario.pedido(Cenario.persona("P3"), LocalDate.of(2026, 10, 20)));
        Reserva.Efeito efeito = cenario.registrar(r.cancelar(false, "Não aprovada", false, Cenario.ADMIN,
                Cenario.AGORA));
        assertEquals(List.of(), efeito.movimentos());
        CotaDoMes cota = cenario.cota(Cenario.persona("P3"), r.pedido().data());
        mesmoValor("0", cota.utilizadas());
        mesmoValor("20", cota.saldo());
        assertThrows(RegraException.class, () -> r.cancelar(false, "De novo", false, Cenario.ADMIN, Cenario.AGORA),
                "cancelada não aceita mais ação");
    }

    @Test
    void forcaMaiorJustificadaDispensaPenalidade() {
        Reserva r = cenario.solicitar(Cenario.pedido(Cenario.persona("P3"), LocalDate.of(2026, 10, 20)));
        cenario.aprovar(r);
        cenario.registrar(r.cancelar(true, "Interdição do espaço", true, Cenario.ADMIN, Cenario.AGORA));
        mesmoValor("0", cenario.cota(Cenario.persona("P3"), r.pedido().data()).utilizadas());
    }

    @Test
    void horasNaoAcumulamEReservaForaDaCotaContinuaPossivel() {
        Persona p3 = new Persona(3, "P3", horas("4"));
        Reserva r = cenario.solicitar(Cenario.pedido(p3, LocalDate.of(2026, 10, 20)));
        cenario.aprovar(r);
        Reserva outra = cenario.solicitar(Cenario.pedido(p3, LocalDate.of(2026, 10, 21)));
        assertTrue(r.orcamento().usaCota());
        assertFalse(outra.orcamento().usaCota());
        assertEquals(100, outra.orcamento().locacao());
        mesmoValor("4", cenario.cota(p3, LocalDate.of(2026, 11, 1)).saldo());
    }

    @Test
    void concluirSoDepoisDoFimEPagamentoSoEmAprovadaOuConcluida() {
        Pedido hoje = Cenario.pedido(Cenario.persona("P6"), LocalDate.of(2026, 10, 5));
        Reserva r = Reserva.solicitar(hoje, cenario.orcamento(Cenario.pedido(Cenario.persona("P6"), LocalDate.of(2026,
                10, 20)), BigDecimal.ZERO), Cenario.AGORA.minusDays(1));
        assertThrows(RegraException.class, () -> r.registrarPagamento(SituacaoPagamento.PAGO, Cenario.ADMIN),
                "pendente não recebe pagamento");
        assertThrows(RegraException.class, () -> r.concluir(Cenario.ADMIN, Cenario.AGORA), "pendente não conclui");
        r.aprovar(cenario.orcamento(hoje, BigDecimal.ZERO), Cenario.ADMIN, Cenario.AGORA.minusDays(1));
        assertEquals("Só é possível concluir uma reserva aprovada após seu término.", assertThrows(
                RegraException.class, () -> r.concluir(Cenario.ADMIN, Cenario.AGORA.minusHours(1))).getMessage());
        Reserva.Efeito conclusao = r.concluir(Cenario.ADMIN, Cenario.AGORA);
        assertEquals(SituacaoReserva.CONCLUIDA, r.situacao());
        assertEquals("CONCLUSÃO", conclusao.acao());
        Reserva.Efeito pagamento = r.registrarPagamento(SituacaoPagamento.PAGO, Cenario.ADMIN);
        assertEquals(SituacaoPagamento.PAGO, r.pagamento());
        assertEquals("PAGAMENTO", pagamento.acao());
        assertEquals("PAGO", pagamento.descricao());
        assertNull(r.motivoRecusa());
    }

    @Test
    void reservaDeDiaPassadoNaoPodeSerSolicitada() {
        assertThrows(RegraException.class, () -> cenario.solicitar(Cenario.pedido(Cenario.persona("P6"), LocalDate.of(
                2026, 10, 5))), "8h às 12h de hoje já começou");
    }
}

package br.com.inpetu.hub.reserva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import br.com.inpetu.hub.comum.RegraException;

/** Preço por turnos, persona, cota com 15 dias de antecedência e taxas. Porta dos testes de preço do antigo. */
class OrcamentoTest {

    private final Cenario cenario = new Cenario();

    @Test
    void externoPagaOTurnoCheioComSinalDe30NumDiaComum() {
        Pedido pedido = Cenario.pedido(Cenario.persona("P6"), LocalDate.of(2026, 10, 20));
        Orcamento o = cenario.orcamento(pedido, BigDecimal.ZERO);
        assertEquals(new BigDecimal("4.00"), o.horas());
        assertEquals(1, o.turnos());
        assertFalse(o.usaCota());
        assertEquals(100, o.locacao());
        assertEquals(new BigDecimal("1600.00"), o.valorFinal());
        assertEquals(30, o.percentualSinal());
        assertEquals(new BigDecimal("480.00"), o.valorSinal());
        assertEquals(new BigDecimal("1120.00"), o.valorRestante());
        assertTrue(o.taxasConfiguradas());
    }

    @Test
    void turnosArredondamParaCimaPeloExpediente() {
        Pedido pedido = new Pedido(Cenario.persona("P6"), 7L, LocalDate.of(2026, 10, 20), LocalTime.of(8, 0), LocalTime
                .of(12, 30), 10, false, false, false, "T", "O", null);
        Orcamento o = cenario.orcamento(pedido, BigDecimal.ZERO);
        assertEquals(new BigDecimal("4.50"), o.horas());
        assertEquals(2, o.turnos());
        assertEquals(new BigDecimal("2800.00"), o.valorFinal());
    }

    @Test
    void menosDe15DiasNaoUtilizaCota() {
        Orcamento o = cenario.orcamento(Cenario.pedido(Cenario.persona("P2"), LocalDate.of(2026, 10, 19)),
                BigDecimal.ZERO);
        assertFalse(o.elegivel());
        assertFalse(o.usaCota());
        assertEquals(100, o.locacao());
    }

    @Test
    void com15DiasEPersonaComCotaUsaACota() {
        Orcamento o = cenario.orcamento(Cenario.pedido(Cenario.persona("P2"), LocalDate.of(2026, 10, 20)),
                BigDecimal.ZERO);
        assertTrue(o.elegivel());
        assertTrue(o.usaCota());
        assertEquals(0, o.locacao(), "P2 acadêmico na cota: sem locação, só limpeza com insumos");
        assertEquals(100, o.limpeza());
    }

    @Test
    void p1NaoPagaNada() {
        Orcamento o = cenario.orcamento(Cenario.pedido(Cenario.persona("P1"), LocalDate.of(2026, 10, 20)),
                BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), o.valorFinal());
        assertEquals(new BigDecimal("1600"), o.valorBase(), "o valor de referência continua o da tabela");
    }

    @Test
    void p2SemPagamentoFinanciamentoNemAtividadeAcademicaERecusado() {
        Pedido pedido = new Pedido(Cenario.persona("P2"), 7L, LocalDate.of(2026, 10, 20), LocalTime.of(8, 0), LocalTime
                .of(12, 0), 10, false, false, false, "T", "O", null);
        assertEquals("Para P2 sem pagamento/financiamento, indique que a atividade é acadêmica.", assertThrows(
                RegraException.class, () -> cenario.orcamento(pedido, BigDecimal.ZERO)).getMessage());
    }

    @Test
    void anoSemTabelaDePrecosDaMensagemAmigavel() {
        Pedido pedido = Cenario.pedido(Cenario.persona("P6"), LocalDate.of(2027, 1, 20));
        assertEquals("Tabela de preços não configurada para 2027 / 1 turno(s). Solicite ao gestor o cadastro.",
                assertThrows(RegraException.class, () -> cenario.orcamento(pedido, BigDecimal.ZERO)).getMessage());
    }

    @Test
    void taxasDeOperacaoELimpezaEntramNoTotalEFaltandoFicamNaoConfiguradas() {
        cenario.configuracao = new Configuracao(4, LocalTime.of(8, 0), LocalTime.of(20, 0), new BigDecimal("100"),
                new BigDecimal("50"), new BigDecimal("80"));
        Orcamento o = cenario.orcamento(Cenario.pedido(Cenario.persona("P6"), LocalDate.of(2026, 10, 25)),
                BigDecimal.ZERO);
        assertEquals(new BigDecimal("100.00"), o.valorOperacao());
        assertEquals(new BigDecimal("50.00"), o.valorLimpeza());
        assertEquals(new BigDecimal("1750.00"), o.valorFinal());
        assertEquals(60, o.percentualSinal(), "domingo");
        assertEquals(new BigDecimal("1050.00"), o.valorSinal());
        cenario.configuracao = new Configuracao(4, LocalTime.of(8, 0), LocalTime.of(20, 0), BigDecimal.ZERO, null,
                BigDecimal.ZERO);
        assertFalse(cenario.orcamento(Cenario.pedido(Cenario.persona("P6"), LocalDate.of(2026, 10, 20)),
                BigDecimal.ZERO).taxasConfiguradas());
    }
}

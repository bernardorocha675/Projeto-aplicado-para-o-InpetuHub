package br.com.inpetu.hub.reserva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

/** Porta do RegrasTest antigo: sobreposição, sinal de 30%/60% e taxação por persona. */
class RegrasTest {

    @Test
    void detectaSobreposicaoEMantemHorariosAdjacentesLivres() {
        assertTrue(Horarios.sobrepoe(LocalTime.of(15, 0), LocalTime.of(17, 0), LocalTime.of(14, 0), LocalTime.of(16,
                0)));
        assertFalse(Horarios.sobrepoe(LocalTime.of(16, 0), LocalTime.of(17, 0), LocalTime.of(14, 0), LocalTime.of(16,
                0)));
    }

    @Test
    void calculaSinalDe30EDe60PorCento() {
        assertEquals(new BigDecimal("480.00"), Orcamentos.sinal(new BigDecimal("1600"), 30));
        assertEquals(new BigDecimal("960.00"), Orcamentos.sinal(new BigDecimal("1600"), 60));
    }

    @Test
    void domingoEFeriadoUsam60EDiaComum30() {
        assertEquals(60, Orcamentos.percentualSinal(LocalDate.of(2026, 10, 11), Cenario.FERIADOS), "domingo");
        assertEquals(60, Orcamentos.percentualSinal(LocalDate.of(2026, 10, 12), Cenario.FERIADOS), "feriado");
        assertEquals(30, Orcamentos.percentualSinal(LocalDate.of(2026, 10, 13), Cenario.FERIADOS));
    }

    @Test
    void taxacaoRespeitaPersonas() {
        assertEquals(0, Taxacao.calcular("P1", false, true, false).locacao());
        assertEquals(50, Taxacao.calcular("P2", true, true, false).locacao());
        assertTrue(Taxacao.calcular("P2", true, false, true).insumos());
        for (String codigo : new String[]{"P3", "P4", "P5"}) {
            assertEquals(75, Taxacao.calcular(codigo, true, true, false).locacao());
            assertEquals(0, Taxacao.calcular(codigo, true, false, false).locacao());
            assertEquals(100, Taxacao.calcular(codigo, false, false, false).locacao());
        }
        assertEquals(100, Taxacao.calcular("P6", false, false, false).locacao());
    }
}

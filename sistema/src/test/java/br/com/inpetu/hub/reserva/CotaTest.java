package br.com.inpetu.hub.reserva;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;

/** A cota do mês é refeita a partir dos movimentos; a dívida passa para o mês seguinte. */
class CotaTest {

    private static final Persona P4 = new Persona(4, "P4", new BigDecimal("6"));

    @Test
    void semMovimentosOSaldoEACotaInteira() {
        CotaDoMes cota = Cota.doMes(P4, List.of(), YearMonth.of(2026, 10));
        assertEquals(0, new BigDecimal("6").compareTo(cota.saldo()));
        assertEquals(0, BigDecimal.ZERO.compareTo(cota.utilizadas()));
    }

    @Test
    void dividaDoMesPassaParaOSeguinte() {
        List<Movimento> movimentos = List.of(new Movimento(TipoMovimento.UTILIZACAO, YearMonth.of(2026, 10),
                new BigDecimal("4"), "a"),
                new Movimento(TipoMovimento.ESTORNO_UTILIZACAO, YearMonth.of(2026, 10),
                        new BigDecimal("-4"), "b"),
                new Movimento(TipoMovimento.PENALIDADE, YearMonth.of(2026, 10),
                        new BigDecimal("8"), "c"));
        CotaDoMes outubro = Cota.doMes(P4, movimentos, YearMonth.of(2026, 10));
        assertEquals(0, new BigDecimal("8").compareTo(outubro.utilizadas()));
        assertEquals(0, new BigDecimal("2").compareTo(outubro.saldoDevedor()));
        assertEquals(0, BigDecimal.ZERO.compareTo(outubro.saldo()));
        CotaDoMes novembro = Cota.doMes(P4, movimentos, YearMonth.of(2026, 11));
        assertEquals(0, new BigDecimal("2").compareTo(novembro.utilizadas()), "a dívida de outubro");
        assertEquals(0, new BigDecimal("4").compareTo(novembro.saldo()));
        assertEquals(0, BigDecimal.ZERO.compareTo(Cota.doMes(P4, movimentos, YearMonth.of(2026, 12)).utilizadas()),
                "a dívida acaba quando cabe na cota");
    }

    @Test
    void horasNaoAcumulamDeUmMesParaOOutro() {
        CotaDoMes novembro = Cota.doMes(P4, List.of(), YearMonth.of(2026, 11));
        assertEquals(0, new BigDecimal("6").compareTo(novembro.saldo()));
    }

    @Test
    void personaSemCotaTemZeroHoras() {
        CotaDoMes cota = Cota.doMes(new Persona(6, "P6", null), List.of(), YearMonth.of(2026, 10));
        assertEquals(0, BigDecimal.ZERO.compareTo(cota.horasTotais()));
        assertEquals(0, BigDecimal.ZERO.compareTo(cota.saldo()));
    }
}

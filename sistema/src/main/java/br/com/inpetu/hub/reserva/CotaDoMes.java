package br.com.inpetu.hub.reserva;

import java.math.BigDecimal;
import java.time.YearMonth;

/** A cota de uma persona num mês: total, horas usadas (com a dívida do mês anterior) e o que passa adiante. */
public record CotaDoMes(YearMonth mes, BigDecimal horasTotais, BigDecimal utilizadas, BigDecimal saldoDevedor) {

    /** Horas que ainda cabem na cota do mês. */
    public BigDecimal saldo() {
        return horasTotais.subtract(utilizadas).max(BigDecimal.ZERO);
    }
}

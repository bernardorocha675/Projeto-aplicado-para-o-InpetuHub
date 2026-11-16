package br.com.inpetu.hub.reserva;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Um movimento de cota no mês de referência (horas negativas devolvem). */
public record Movimento(TipoMovimento tipo, YearMonth mes, BigDecimal horas, String descricao) {
}

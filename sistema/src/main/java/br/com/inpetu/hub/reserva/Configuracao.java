package br.com.inpetu.hub.reserva;

import java.math.BigDecimal;
import java.time.LocalTime;

/** Configuração do Hub: turno, expediente e taxas (taxa nula = ainda não configurada). */
public record Configuracao(int horasPorTurno, LocalTime abertura, LocalTime fechamento, BigDecimal taxaOperacao,
        BigDecimal taxaLimpeza, BigDecimal insumosLimpeza) {
}

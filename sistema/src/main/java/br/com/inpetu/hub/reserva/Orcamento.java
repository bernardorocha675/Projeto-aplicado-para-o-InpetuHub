package br.com.inpetu.hub.reserva;

import java.math.BigDecimal;

/** O preço calculado de um pedido: horas, turnos, cota, percentuais, valores e sinal. */
public record Orcamento(BigDecimal horas, int turnos, boolean elegivel, boolean usaCota, int locacao, int operacao,
        int limpeza, BigDecimal valorBase, BigDecimal valorOperacao, BigDecimal valorLimpeza, BigDecimal valorFinal,
        int percentualSinal, BigDecimal valorSinal, BigDecimal valorRestante, boolean taxasConfiguradas) {

    /** Horas que a reserva tira da cota quando aprovada. */
    public BigDecimal horasCota() {
        return usaCota ? horas : BigDecimal.ZERO;
    }
}

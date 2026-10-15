package br.com.inpetu.hub.dto;

import br.com.inpetu.hub.entity.CotaMensal;
import java.math.BigDecimal;

public record Orcamento(
    BigDecimal horas,
    boolean elegivel,
    boolean usaCota,
    CotaMensal cota,
    int locacao,
    int operacao,
    int limpeza,
    BigDecimal valorBase,
    BigDecimal valorOperacao,
    BigDecimal valorLimpeza,
    BigDecimal valorFinal,
    int percentualSinal,
    BigDecimal valorSinal,
    BigDecimal valorRestante,
    boolean taxasConfiguradas,
    int turnos) {}

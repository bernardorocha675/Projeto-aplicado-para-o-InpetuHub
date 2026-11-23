package br.com.inpetu.hub.reserva;

/**
 * Movimentos de cota. Só a aprovação usa cota (pendentes nunca reservam horas), por isso os tipos de reserva provisória
 * do antigo não existem aqui.
 */
public enum TipoMovimento {
    UTILIZACAO, ESTORNO_UTILIZACAO, PENALIDADE
}

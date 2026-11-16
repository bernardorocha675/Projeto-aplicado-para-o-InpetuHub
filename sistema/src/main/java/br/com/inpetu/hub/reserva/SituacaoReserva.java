package br.com.inpetu.hub.reserva;

/** Situação da reserva, gravada pelo nome (os mesmos nomes do antigo). Só APROVADA ocupa horário e cota. */
public enum SituacaoReserva {
    PENDENTE, APROVADA, RECUSADA, CANCELADA, CONCLUIDA
}

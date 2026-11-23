package br.com.inpetu.hub.reserva;

import java.math.BigDecimal;

/** Persona do pedido (P1 a P6). Sem cota mensal ({@code null}), a reserva nunca usa cota. */
public record Persona(long id, String codigo, BigDecimal cotaMensalHoras) {
}

package br.com.inpetu.hub.reserva;

import java.time.LocalDate;
import java.time.LocalTime;

/** O que se pede numa reserva: persona, espaço, dia, horário e os dados que mudam o preço. */
public record Pedido(Persona persona, long espacoId, LocalDate data, LocalTime inicio, LocalTime fim,
        int participantes, boolean inscricaoPaga, boolean financiado, boolean academico, String titulo, String objetivo,
        String observacoes) {
}

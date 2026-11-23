package br.com.inpetu.hub.reserva;

import java.time.LocalTime;

/** Um horário ocupado num dia. */
public record Intervalo(LocalTime inicio, LocalTime fim) {
}

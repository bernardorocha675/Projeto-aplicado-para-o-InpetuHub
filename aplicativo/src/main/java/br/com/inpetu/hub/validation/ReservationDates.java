package br.com.inpetu.hub.validation;

import java.time.*;

/** Usa o mesmo relógio/fuso da aplicação na regra e nos limites exibidos. */
public final class ReservationDates {
  private ReservationDates() {}
  public static LocalDate lastDay(Clock clock) {
    return LocalDate.of(LocalDate.now(clock).getYear()+1,12,31);
  }
}

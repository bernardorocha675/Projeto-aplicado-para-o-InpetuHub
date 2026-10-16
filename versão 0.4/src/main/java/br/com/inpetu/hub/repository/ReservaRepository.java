package br.com.inpetu.hub.repository;

import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.enums.*;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
  List<Reserva> findByDataBetweenOrderByDataAscHoraInicioAsc(LocalDate inicio, LocalDate fim);

  List<Reserva> findByEmpreendedorIdOrderByDataDesc(Long id);

  @Query(
      "select r from Reserva r where r.espaco.id=:espaco and r.data=:data and r.statusReserva in"
          + " :status and r.horaInicio<:fim and r.horaFim>:inicio and (:ignorar is null or"
          + " r.id<>:ignorar)")
  List<Reserva> conflitos(
      @Param("espaco") Long espaco,
      @Param("data") LocalDate data,
      @Param("inicio") LocalTime inicio,
      @Param("fim") LocalTime fim,
      @Param("ignorar") Long ignorar,
      @Param("status") List<StatusReserva> status);
}

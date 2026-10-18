package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.repository.*;
import br.com.inpetu.hub.service.DisponibilidadeService;
import br.com.inpetu.hub.exception.RegraException;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReservationDatesTest {
 DisponibilidadeService service(String instant){
  var config=mock(ConfiguracaoRepository.class);var rooms=mock(EspacoRepository.class);
  var c=new Configuracao();c.abertura=LocalTime.of(8,0);c.fechamento=LocalTime.of(18,0);
  var e=new Espaco();e.ativo=true;e.disponibilidade=true;e.capacidade=20;
  when(config.findById(1L)).thenReturn(Optional.of(c));when(rooms.findById(1L)).thenReturn(Optional.of(e));
  return new DisponibilidadeService(mock(ReservaRepository.class),rooms,config,Clock.fixed(Instant.parse(instant),ZoneId.of("America/Sao_Paulo")));
 }
 void check(DisponibilidadeService s,String date){s.validar(1L,LocalDate.parse(date),LocalTime.of(9,0),LocalTime.of(10,0),5,null);}
 @Test void endOfFollowingCalendarYearIsAllowedBeyond365Days(){
  var s=service("2026-10-18T15:00:00Z");assertDoesNotThrow(()->check(s,"2027-12-31"));assertThrows(RegraException.class,()->check(s,"2028-01-01"));
 }
 @Test void yearWindowMovesAutomaticallyAtMidnightInApplicationTimezone(){
  var before=service("2027-01-01T02:59:59Z");assertThrows(RegraException.class,()->check(before,"2028-12-31"));
  var after=service("2027-01-01T03:00:00Z");assertDoesNotThrow(()->check(after,"2028-12-31"));
 }
 @Test void pastDatesAndTimesRemainForbidden(){
  var s=service("2026-10-18T15:00:00Z");assertThrows(RegraException.class,()->check(s,"2026-10-17"));assertThrows(RegraException.class,()->check(s,"2026-10-18"));
 }
}

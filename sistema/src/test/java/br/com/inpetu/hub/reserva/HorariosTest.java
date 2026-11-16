package br.com.inpetu.hub.reserva;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.inpetu.hub.comum.RegraException;
import br.com.inpetu.hub.espaco.Espaco;

/** Porta do ReservationDatesTest e das conferências de horário do FluxosIntegrationTest. */
class HorariosTest {

    private static final Configuracao EXPEDIENTE = new Configuracao(4, LocalTime.of(8, 0), LocalTime.of(18, 0),
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    private static final Espaco SALA = new Espaco(1L, "Sala", 20, null, true, true);

    private static Clock em(String instante) {
        return Clock.fixed(Instant.parse(instante), ZoneId.of("America/Sao_Paulo"));
    }

    private static void conferir(Clock relogio, String data) {
        Horarios.validar(SALA, LocalDate.parse(data), LocalTime.of(9, 0), LocalTime.of(10, 0), 5, EXPEDIENTE, List.of(),
                relogio);
    }

    @Test
    void fimDoAnoSeguinteEPermitidoAlemDe365Dias() {
        Clock relogio = em("2026-10-18T15:00:00Z");
        assertDoesNotThrow(() -> conferir(relogio, "2027-12-31"));
        RegraException erro = assertThrows(RegraException.class, () -> conferir(relogio, "2028-01-01"));
        assertEquals("Selecione uma data no ano atual ou no próximo ano.", erro.getMessage());
        assertEquals(LocalDate.of(2027, 12, 31), Horarios.ultimoDia(relogio));
    }

    @Test
    void janelaAndaSozinhaAMeiaNoiteNoFusoDoSistema() {
        assertThrows(RegraException.class, () -> conferir(em("2027-01-01T02:59:59Z"), "2028-12-31"));
        assertDoesNotThrow(() -> conferir(em("2027-01-01T03:00:00Z"), "2028-12-31"));
    }

    @Test
    void diasEHorariosPassadosContinuamProibidos() {
        Clock relogio = em("2026-10-18T15:00:00Z");
        assertThrows(RegraException.class, () -> conferir(relogio, "2026-10-17"));
        RegraException erro = assertThrows(RegraException.class, () -> conferir(relogio, "2026-10-18"));
        assertEquals("Não é possível reservar no passado.", erro.getMessage());
    }

    @Test
    void horarioForaDoExpedienteOuInvertidoERecusado() {
        Clock relogio = em("2026-10-18T15:00:00Z");
        LocalDate dia = LocalDate.of(2026, 10, 20);
        assertEquals("O horário final deve ser posterior ao inicial.", assertThrows(RegraException.class,
                () -> Horarios.validar(SALA, dia, LocalTime.of(10, 0), LocalTime.of(10, 0), 5, EXPEDIENTE, List.of(),
                        relogio))
                .getMessage());
        assertEquals("Selecione um horário dentro do expediente: 08:00–18:00", assertThrows(RegraException.class,
                () -> Horarios.validar(SALA, dia, LocalTime.of(7, 0), LocalTime.of(10, 0), 5, EXPEDIENTE, List.of(),
                        relogio))
                .getMessage());
        assertThrows(RegraException.class, () -> Horarios.validar(SALA, dia, LocalTime.of(17, 0), LocalTime.of(19, 0),
                5, EXPEDIENTE, List.of(), relogio));
    }

    @Test
    void capacidadeEEspacoInativoOuIndisponivelSaoConferidos() {
        Clock relogio = em("2026-10-18T15:00:00Z");
        LocalDate dia = LocalDate.of(2026, 10, 20);
        assertEquals("O número de participantes deve ficar entre 1 e 20.", assertThrows(RegraException.class,
                () -> Horarios.validar(SALA, dia, LocalTime.of(9, 0), LocalTime.of(10, 0), 21, EXPEDIENTE, List.of(),
                        relogio))
                .getMessage());
        assertThrows(RegraException.class, () -> Horarios.validar(SALA, dia, LocalTime.of(9, 0), LocalTime.of(10, 0),
                0, EXPEDIENTE, List.of(), relogio));
        for (Espaco fora : new Espaco[]{new Espaco(1L, "Sala", 20, null, true, false), new Espaco(1L, "Sala", 20, null,
                false, true)}) {
            assertEquals("Este espaço está indisponível.", assertThrows(RegraException.class, () -> Horarios.validar(
                    fora, dia, LocalTime.of(9, 0), LocalTime.of(10, 0), 5, EXPEDIENTE, List.of(), relogio))
                    .getMessage());
        }
    }

    @Test
    void reservaAprovadaNoMesmoEspacoBloqueiaSoOQueSobrepoe() {
        Clock relogio = em("2026-10-18T15:00:00Z");
        LocalDate dia = LocalDate.of(2026, 10, 20);
        List<Intervalo> aprovadas = List.of(new Intervalo(LocalTime.of(8, 0), LocalTime.of(12, 0)));
        assertEquals("Horário indisponível: já existe uma reserva neste intervalo.", assertThrows(
                RegraException.class, () -> Horarios.validar(SALA, dia, LocalTime.of(11, 0), LocalTime.of(14, 0), 5,
                        EXPEDIENTE, aprovadas, relogio))
                .getMessage());
        assertDoesNotThrow(() -> Horarios.validar(SALA, dia, LocalTime.of(12, 0), LocalTime.of(14, 0), 5, EXPEDIENTE,
                aprovadas, relogio), "horário colado ao fim fica livre");
    }
}

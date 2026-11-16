package br.com.inpetu.hub.reserva;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import br.com.inpetu.hub.espaco.Espaco;

/**
 * Os dados iniciais da V1 (personas, configuração com taxas zeradas, preços de 2026 e feriados) e o relógio do
 * FluxosIntegrationTest antigo: 05/10/2026, 12h em Brasília. Guarda os movimentos de cota como o banco guardaria.
 */
final class Cenario {

    static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-10-05T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    static final LocalDateTime AGORA = LocalDateTime.now(RELOGIO);
    static final Set<LocalDate> FERIADOS = Set.of(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 11, 2), LocalDate
            .of(2026, 11, 15), LocalDate.of(2026, 11, 20), LocalDate.of(2026, 12, 25));
    static final long ADMIN = 1L;
    static final Espaco SALA = new Espaco(7L, "Sala de teste", 40, null, true, true);

    Configuracao configuracao = new Configuracao(4, LocalTime.of(8, 0), LocalTime.of(20, 0), BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO);
    final TabelaPrecos precos = new TabelaPrecos();
    final List<Movimento> movimentos = new ArrayList<>();

    Cenario() {
        int[][] valores = {{1, 100, 1600}, {1, 75, 1200}, {1, 50, 800}, {2, 100, 2800}, {2, 75, 2100}, {2, 50, 1400},
                {3, 100, 3600}, {3, 75, 2700}, {3, 50, 1800}};
        for (int[] v : valores) {
            precos.adicionar(2026, v[0], v[1], BigDecimal.valueOf(v[2]));
        }
    }

    static Persona persona(String codigo) {
        return switch (codigo) {
            case "P1" -> new Persona(1, "P1", null);
            case "P2" -> new Persona(2, "P2", new BigDecimal("30"));
            case "P3" -> new Persona(3, "P3", new BigDecimal("20"));
            case "P4" -> new Persona(4, "P4", new BigDecimal("10"));
            case "P5" -> new Persona(5, "P5", new BigDecimal("10"));
            default -> new Persona(6, "P6", null);
        };
    }

    /** Pedido das 8h às 12h para 10 pessoas, acadêmico, como o form() do teste antigo. */
    static Pedido pedido(Persona persona, LocalDate data) {
        return new Pedido(persona, SALA.id(), data, LocalTime.of(8, 0), LocalTime.of(12, 0), 10, false, false, true,
                "Teste", "Conectar", null);
    }

    CotaDoMes cota(Persona persona, LocalDate dia) {
        return Cota.doMes(persona, movimentos, java.time.YearMonth.from(dia));
    }

    Orcamento orcamento(Pedido pedido, BigDecimal creditoProprio) {
        return Orcamentos.calcular(pedido, cota(pedido.persona(), pedido.data()), creditoProprio, AGORA.toLocalDate(),
                configuracao, precos, FERIADOS);
    }

    /** Solicita como o serviço fará: confere o horário, calcula e cria a reserva pendente. */
    Reserva solicitar(Pedido pedido, List<Intervalo> aprovadas) {
        Horarios.validar(SALA, pedido.data(), pedido.inicio(), pedido.fim(), pedido.participantes(), configuracao,
                aprovadas, RELOGIO);
        return Reserva.solicitar(pedido, orcamento(pedido, BigDecimal.ZERO), AGORA);
    }

    Reserva solicitar(Pedido pedido) {
        return solicitar(pedido, List.of());
    }

    /** Aplica o efeito de uma ação: os movimentos de cota vão para o "banco". */
    Reserva.Efeito registrar(Reserva.Efeito efeito) {
        movimentos.addAll(efeito.movimentos());
        return efeito;
    }

    Reserva.Efeito aprovar(Reserva reserva) {
        return registrar(reserva.aprovar(orcamento(reserva.pedido(), BigDecimal.ZERO), ADMIN, AGORA));
    }
}

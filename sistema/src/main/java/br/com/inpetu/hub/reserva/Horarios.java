package br.com.inpetu.hub.reserva;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import br.com.inpetu.hub.comum.RegraException;
import br.com.inpetu.hub.espaco.Espaco;

/**
 * Conferência do horário pedido: depois de agora, até 31/12 do ano seguinte (no fuso do relógio), dentro do expediente,
 * num espaço reservável com capacidade e sem sobrepor uma reserva APROVADA do mesmo espaço.
 */
public final class Horarios {

    private Horarios() {
    }

    /** Último dia aceito para novas reservas: 31/12 do ano seguinte. */
    public static LocalDate ultimoDia(Clock relogio) {
        return LocalDate.of(LocalDate.now(relogio).getYear() + 1, 12, 31);
    }

    public static boolean sobrepoe(LocalTime inicio, LocalTime fim, LocalTime outroInicio, LocalTime outroFim) {
        return inicio.isBefore(outroFim) && fim.isAfter(outroInicio);
    }

    /** {@code aprovadas}: as reservas aprovadas do mesmo espaço e dia (sem a própria, numa alteração). */
    public static void validar(Espaco espaco, LocalDate data, LocalTime inicio, LocalTime fim, int participantes,
            Configuracao configuracao, List<Intervalo> aprovadas, Clock relogio) {
        if (data == null || inicio == null || fim == null || !fim.isAfter(inicio)) {
            throw new RegraException("O horário final deve ser posterior ao inicial.");
        }
        if (LocalDateTime.of(data, inicio).isBefore(LocalDateTime.now(relogio))) {
            throw new RegraException("Não é possível reservar no passado.");
        }
        if (data.isAfter(ultimoDia(relogio))) {
            throw new RegraException("Selecione uma data no ano atual ou no próximo ano.");
        }
        if (inicio.isBefore(configuracao.abertura()) || fim.isAfter(configuracao.fechamento())) {
            throw new RegraException("Selecione um horário dentro do expediente: " + configuracao.abertura() + "–"
                    + configuracao.fechamento());
        }
        if (espaco == null || !espaco.reservavel()) {
            throw new RegraException("Este espaço está indisponível.");
        }
        if (participantes < 1 || participantes > espaco.capacidade()) {
            throw new RegraException("O número de participantes deve ficar entre 1 e " + espaco.capacidade() + ".");
        }
        for (Intervalo ocupado : aprovadas) {
            if (sobrepoe(inicio, fim, ocupado.inicio(), ocupado.fim())) {
                throw new RegraException("Horário indisponível: já existe uma reserva neste intervalo.");
            }
        }
    }
}

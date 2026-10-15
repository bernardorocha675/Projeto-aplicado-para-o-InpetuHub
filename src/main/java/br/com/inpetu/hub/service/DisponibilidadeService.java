package br.com.inpetu.hub.service;

import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.enums.*;
import br.com.inpetu.hub.exception.RegraException;
import br.com.inpetu.hub.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class DisponibilidadeService {
  private final ReservaRepository reservas;
  private final EspacoRepository espacos;
  private final ConfiguracaoRepository config;
  private final Clock clock;

  public DisponibilidadeService(
      ReservaRepository r, EspacoRepository e, ConfiguracaoRepository c, Clock clock) {
    reservas = r;
    espacos = e;
    config = c;
    this.clock = clock;
  }

  public static boolean sobrepoe(
      LocalTime inicio, LocalTime fim, LocalTime existenteInicio, LocalTime existenteFim) {
    return inicio.isBefore(existenteFim) && fim.isAfter(existenteInicio);
  }

  public Espaco validar(
      Long id, LocalDate data, LocalTime inicio, LocalTime fim, int participantes, Long ignorar) {
    if (data == null || inicio == null || fim == null || !fim.isAfter(inicio))
      throw new RegraException("O horário final deve ser posterior ao inicial.");
    if (LocalDateTime.of(data, inicio).isBefore(LocalDateTime.now(clock)))
      throw new RegraException("Não é possível reservar no passado.");
    Configuracao c = config.findById(1L).orElseThrow();
    if (inicio.isBefore(c.abertura) || fim.isAfter(c.fechamento))
      throw new RegraException(
          "Selecione um horário dentro do expediente: " + c.abertura + "–" + c.fechamento);
    Espaco e = espacos.findById(id).orElseThrow(() -> new RegraException("Espaço não encontrado."));
    if (!e.ativo || !e.disponibilidade) throw new RegraException("Este espaço está indisponível.");
    if (participantes < 1 || participantes > e.capacidade)
      throw new RegraException(
          "O número de participantes deve ficar entre 1 e " + e.capacidade + ".");
    if (!reservas
        .conflitos(
            id, data, inicio, fim, ignorar, List.of(StatusReserva.PENDENTE, StatusReserva.APROVADA))
        .isEmpty())
      throw new RegraException("Horário indisponível: já existe uma reserva neste intervalo.");
    return e;
  }
}

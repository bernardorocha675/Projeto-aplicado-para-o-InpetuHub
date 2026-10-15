package br.com.inpetu.hub.service;

import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.enums.*;
import br.com.inpetu.hub.repository.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class CotaService {
  private final CotaMensalRepository cotas;
  private final MovimentacaoCotaRepository movimentos;
  private final Clock clock;

  public CotaService(CotaMensalRepository c, MovimentacaoCotaRepository m, Clock clock) {
    cotas = c;
    movimentos = m;
    this.clock = clock;
  }

  public CotaMensal consultar(Persona persona, LocalDate data) {
    return reconstruir(persona, YearMonth.from(data), false);
  }

  public CotaMensal reconstruir(Persona persona, YearMonth alvo, boolean salvar) {
    var registros = movimentos.findByPersonaIdOrderByMesReferenciaAscIdAsc(persona.id);
    var existentes = cotas.findByPersonaIdOrderByAnoAscMesAsc(persona.id);
    YearMonth inicio = alvo, fim = alvo;
    for (var m : registros) {
      var ym = YearMonth.from(m.mesReferencia);
      if (ym.isBefore(inicio)) inicio = ym;
      if (ym.isAfter(fim)) fim = ym;
    }
    for (var c : existentes) {
      var ym = YearMonth.of(c.ano, c.mes);
      if (ym.isBefore(inicio)) inicio = ym;
      if (ym.isAfter(fim)) fim = ym;
    }
    BigDecimal divida = BigDecimal.ZERO;
    CotaMensal resultado = null;
    for (YearMonth ym = inicio; !ym.isAfter(fim); ym = ym.plusMonths(1)) {
      var y = ym;
      CotaMensal c =
          existentes.stream()
              .filter(x -> x.ano == y.getYear() && x.mes == y.getMonthValue())
              .findFirst()
              .orElseGet(CotaMensal::new);
      c.persona = persona;
      c.ano = ym.getYear();
      c.mes = ym.getMonthValue();
      if (c.id == null)
        c.horasTotais = persona.cotaMensalHoras == null ? BigDecimal.ZERO : persona.cotaMensalHoras;
      c.horasUtilizadas = divida;
      c.horasReservadasPendentes = BigDecimal.ZERO;
      for (var m : registros)
        if (YearMonth.from(m.mesReferencia).equals(ym)) {
          switch (m.tipo) {
            case RESERVA_PENDENTE, LIBERACAO_PENDENTE ->
                c.horasReservadasPendentes = c.horasReservadasPendentes.add(m.quantidadeHoras);
            default -> c.horasUtilizadas = c.horasUtilizadas.add(m.quantidadeHoras);
          }
        }
      c.horasReservadasPendentes = c.horasReservadasPendentes.max(BigDecimal.ZERO);
      c.horasUtilizadas = c.horasUtilizadas.max(BigDecimal.ZERO);
      divida = c.horasUtilizadas.subtract(c.horasTotais).max(BigDecimal.ZERO);
      c.saldoDevedor = divida;
      if (salvar) cotas.save(c);
      if (ym.equals(alvo)) resultado = c;
    }
    return resultado;
  }

  public void movimentar(Reserva r, TipoMovimentacao tipo, BigDecimal horas, String descricao) {
    if (horas.signum() == 0) return;
    MovimentacaoCota m = new MovimentacaoCota();
    m.persona = r.persona;
    m.reserva = r;
    m.dataHora = LocalDateTime.now(clock);
    m.mesReferencia =
        (tipo == TipoMovimentacao.PENALIDADE ? LocalDate.now(clock) : r.data).withDayOfMonth(1);
    m.tipo = tipo;
    m.quantidadeHoras = horas;
    m.descricao = descricao;
    movimentos.saveAndFlush(m);
    reconstruir(r.persona, YearMonth.from(m.mesReferencia), true);
  }

  public void liberar(Reserva r) {
    if (!r.usaCota) return;
    movimentar(
        r,
        r.statusReserva == StatusReserva.PENDENTE
            ? TipoMovimentacao.LIBERACAO_PENDENTE
            : TipoMovimentacao.ESTORNO_UTILIZACAO,
        r.horasCota.negate(),
        "Liberação da reserva #" + r.id);
  }
}

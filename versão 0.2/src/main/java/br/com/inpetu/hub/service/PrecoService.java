package br.com.inpetu.hub.service;

import br.com.inpetu.hub.dto.Orcamento;
import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.exception.RegraException;
import br.com.inpetu.hub.repository.*;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;

@Service
public class PrecoService {
  private final ConfiguracaoRepository config;
  private final TabelaPrecoRepository precos;
  private final FeriadoRepository feriados;
  private final CotaService cotas;
  private final TaxacaoService taxas;

  public PrecoService(
      ConfiguracaoRepository c,
      TabelaPrecoRepository p,
      FeriadoRepository f,
      CotaService q,
      TaxacaoService t) {
    config = c;
    precos = p;
    feriados = f;
    cotas = q;
    taxas = t;
  }

  public static BigDecimal dinheiro(BigDecimal valor) {
    return valor.setScale(2, RoundingMode.HALF_UP);
  }

  public static BigDecimal sinal(BigDecimal valor, int percentual) {
    return dinheiro(valor.multiply(BigDecimal.valueOf(percentual)).divide(BigDecimal.valueOf(100)));
  }

  public int percentualSinal(LocalDate data) {
    return data.getDayOfWeek() == DayOfWeek.SUNDAY || feriados.existsByDataAndAtivoTrue(data)
        ? 60
        : 30;
  }

  public Orcamento calcular(Reserva r, BigDecimal creditoProprio) {
    Configuracao c = config.findById(1L).orElseThrow();
    BigDecimal horas =
        BigDecimal.valueOf(Duration.between(r.horaInicio, r.horaFim).toMinutes())
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    int turnos = (int) Math.ceil(horas.doubleValue() / c.horasPorTurno);
    CotaMensal quota = cotas.consultar(r.persona, r.data);
    boolean elegivel =
        r.persona.cotaMensalHoras != null
            && ChronoUnit.DAYS.between(r.dataHoraSolicitacao.toLocalDate(), r.data) >= 15;
    boolean usa =
        elegivel
            && quota
                    .horasTotais
                    .subtract(quota.horasUtilizadas)
                    .subtract(quota.horasReservadasPendentes)
                    .add(creditoProprio)
                    .max(BigDecimal.ZERO)
                    .compareTo(horas)
                >= 0;
    if (r.persona.codigo.equals("P2") && !r.inscricaoPaga && !r.financiado && !r.academico)
      throw new RegraException(
          "Para P2 sem pagamento/financiamento, indique que a atividade é acadêmica.");
    var t = taxas.calcular(r.persona.codigo, usa, r.inscricaoPaga || r.financiado, r.academico);
    BigDecimal base =
        precos
            .findByAnoAndQuantidadeTurnosAndPercentualAndAtivoTrue(r.data.getYear(), turnos, 100)
            .orElseThrow(
                () ->
                    new RegraException(
                        "Tabela de preços não configurada para "
                            + r.data.getYear()
                            + " / "
                            + turnos
                            + " turno(s). Solicite ao gestor o cadastro."))
            .valor;
    BigDecimal aluguel =
        t.locacao() == 0
            ? BigDecimal.ZERO
            : precos
                .findByAnoAndQuantidadeTurnosAndPercentualAndAtivoTrue(
                    r.data.getYear(), turnos, t.locacao())
                .orElseThrow(
                    () ->
                        new RegraException("Preço não cadastrado para este percentual de locação."))
                .valor;
    BigDecimal limpezaBase = t.insumos() ? c.insumosLimpeza : c.taxaLimpeza;
    boolean configuradas =
        (t.operacao() == 0 || c.taxaOperacao != null) && (t.limpeza() == 0 || limpezaBase != null);
    BigDecimal operacao =
        t.operacao() == 0
            ? BigDecimal.ZERO
            : sinal(c.taxaOperacao == null ? BigDecimal.ZERO : c.taxaOperacao, t.operacao());
    BigDecimal limpeza =
        t.limpeza() == 0
            ? BigDecimal.ZERO
            : sinal(limpezaBase == null ? BigDecimal.ZERO : limpezaBase, t.limpeza());
    BigDecimal total = dinheiro(aluguel.add(operacao).add(limpeza));
    int ps = percentualSinal(r.data);
    BigDecimal sinal = sinal(total, ps);
    return new Orcamento(
        horas,
        elegivel,
        usa,
        quota,
        t.locacao(),
        t.operacao(),
        t.limpeza(),
        base,
        operacao,
        limpeza,
        total,
        ps,
        sinal,
        total.subtract(sinal),
        configuradas,
        turnos);
  }
}

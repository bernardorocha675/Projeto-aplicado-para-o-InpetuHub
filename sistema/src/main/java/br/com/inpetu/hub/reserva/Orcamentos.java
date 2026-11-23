package br.com.inpetu.hub.reserva;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import br.com.inpetu.hub.comum.RegraException;

/**
 * Preço como no antigo: turnos arredondados para cima, valor da tabela pelo percentual de locação da persona, taxas de
 * operação e limpeza, e sinal de 30% (60% em domingo ou feriado). A cota só vale com 15 dias de antecedência e se as
 * horas couberem no saldo do mês.
 */
public final class Orcamentos {

    private static final int DIAS_PARA_COTA = 15;

    private Orcamentos() {
    }

    public static BigDecimal dinheiro(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal sinal(BigDecimal valor, int percentual) {
        return dinheiro(valor.multiply(BigDecimal.valueOf(percentual)).divide(BigDecimal.valueOf(100)));
    }

    public static int percentualSinal(LocalDate data, Set<LocalDate> feriados) {
        return data.getDayOfWeek() == DayOfWeek.SUNDAY || feriados.contains(data) ? 60 : 30;
    }

    /**
     * {@code cota}: a cota da persona no mês do pedido. {@code creditoProprio}: horas que a própria reserva devolve à
     * cota ao ser alterada (zero num pedido novo). {@code solicitadaEm}: dia do pedido, para os 15 dias da cota.
     */
    public static Orcamento calcular(Pedido pedido, CotaDoMes cota, BigDecimal creditoProprio, LocalDate solicitadaEm,
            Configuracao configuracao, TabelaPrecos precos, Set<LocalDate> feriados) {
        Persona persona = pedido.persona();
        BigDecimal horas = BigDecimal.valueOf(Duration.between(pedido.inicio(), pedido.fim()).toMinutes()).divide(
                BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        int turnos = horas.divide(BigDecimal.valueOf(configuracao.horasPorTurno()), 0, RoundingMode.CEILING)
                .intValueExact();
        boolean elegivel = persona.cotaMensalHoras() != null && ChronoUnit.DAYS.between(solicitadaEm, pedido
                .data()) >= DIAS_PARA_COTA;
        boolean usa = elegivel && cota.horasTotais().subtract(cota.utilizadas()).add(creditoProprio).max(
                BigDecimal.ZERO).compareTo(horas) >= 0;
        if (persona.codigo().equals("P2") && !pedido.inscricaoPaga() && !pedido.financiado() && !pedido.academico()) {
            throw new RegraException("Para P2 sem pagamento/financiamento, indique que a atividade é acadêmica.");
        }
        Taxacao.Taxas taxas = Taxacao.calcular(persona.codigo(), usa, pedido.inscricaoPaga() || pedido.financiado(),
                pedido.academico());
        int ano = pedido.data().getYear();
        BigDecimal base = precos.valor(ano, turnos, 100)
                .orElseThrow(() -> new RegraException("Tabela de preços não configurada para " + ano + " / " + turnos
                        + " turno(s). Solicite ao gestor o cadastro."));
        BigDecimal aluguel = taxas.locacao() == 0
                ? BigDecimal.ZERO
                : precos.valor(ano, turnos, taxas.locacao())
                        .orElseThrow(() -> new RegraException("Preço não cadastrado para este percentual de locação."));
        BigDecimal limpezaBase = taxas.insumos() ? configuracao.insumosLimpeza() : configuracao.taxaLimpeza();
        boolean configuradas = (taxas.operacao() == 0 || configuracao.taxaOperacao() != null) && (taxas
                .limpeza() == 0 || limpezaBase != null);
        BigDecimal operacao = taxas.operacao() == 0
                ? BigDecimal.ZERO
                : sinal(zeroSeNulo(configuracao.taxaOperacao()), taxas.operacao());
        BigDecimal limpeza = taxas.limpeza() == 0 ? BigDecimal.ZERO : sinal(zeroSeNulo(limpezaBase), taxas.limpeza());
        BigDecimal total = dinheiro(aluguel.add(operacao).add(limpeza));
        int percentual = percentualSinal(pedido.data(), feriados);
        BigDecimal sinal = sinal(total, percentual);
        return new Orcamento(horas, turnos, elegivel, usa, taxas.locacao(), taxas.operacao(), taxas.limpeza(), base,
                operacao, limpeza, total, percentual, sinal, total.subtract(sinal), configuradas);
    }

    private static BigDecimal zeroSeNulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}

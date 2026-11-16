package br.com.inpetu.hub.reserva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import br.com.inpetu.hub.comum.RegraException;

/**
 * Uma reserva e as suas regras de mudança de situação. Cada ação devolve um {@link Efeito} com o texto do histórico e
 * os movimentos de cota que o serviço grava. A conferência do horário ({@link Horarios#validar}) e o cálculo do
 * {@link Orcamento} vêm de fora, porque dependem do banco.
 */
public final class Reserva {

    private static final int PRAZO_PENDENTE_HORAS = 24;
    private static final int DIAS_SEM_PENALIDADE = 30;
    private static final int MAXIMO_TEXTO = 2000;

    /** O que uma ação produz: ação e texto do histórico e os movimentos de cota a gravar. */
    public record Efeito(String acao, String descricao, List<Movimento> movimentos) {
    }

    private Pedido pedido;
    private Orcamento orcamento;
    private SituacaoReserva situacao;
    private SituacaoPagamento pagamento;
    private final LocalDateTime solicitadaEm;
    private final LocalDateTime expiraEm;
    private String motivoRecusa;
    private Long responsavel;

    private Reserva(Pedido pedido, Orcamento orcamento, LocalDateTime solicitadaEm, LocalDateTime expiraEm) {
        this.pedido = Objects.requireNonNull(pedido);
        this.orcamento = Objects.requireNonNull(orcamento);
        this.situacao = SituacaoReserva.PENDENTE;
        this.pagamento = SituacaoPagamento.PENDENTE;
        this.solicitadaEm = solicitadaEm;
        this.expiraEm = expiraEm;
    }

    /** A reserva como está gravada no banco. */
    public static Reserva restaurar(Pedido pedido, Orcamento orcamento, SituacaoReserva situacao,
            SituacaoPagamento pagamento, LocalDateTime solicitadaEm, LocalDateTime expiraEm, String motivoRecusa,
            Long responsavel) {
        Reserva reserva = new Reserva(pedido, orcamento, solicitadaEm, expiraEm);
        reserva.situacao = Objects.requireNonNull(situacao);
        reserva.pagamento = Objects.requireNonNull(pagamento);
        reserva.motivoRecusa = motivoRecusa;
        reserva.responsavel = responsavel;
        return reserva;
    }

    /** Pedido conferido: nasce pendente, sem ocupar horário nem cota, e expira em 24 h se não for aprovado. */
    public static Reserva solicitar(Pedido pedido, Orcamento orcamento, LocalDateTime agora) {
        return new Reserva(pedido, orcamento, agora, agora.plusHours(PRAZO_PENDENTE_HORAS));
    }

    /** {@code orcamento}: recalculado agora (preço e cota podem ter mudado desde o pedido). */
    public Efeito aprovar(Orcamento recalculado, long colaborador, LocalDateTime agora) {
        if (situacao != SituacaoReserva.PENDENTE) {
            throw new RegraException("Somente reservas pendentes podem ser aprovadas.");
        }
        if (expiraEm != null && expiraEm.isBefore(agora)) {
            throw new RegraException("Reserva pendente expirada.");
        }
        if (!recalculado.taxasConfiguradas()) {
            throw new RegraException("Configure os valores de operação/limpeza na Administração antes de aprovar.");
        }
        orcamento = recalculado;
        situacao = SituacaoReserva.APROVADA;
        responsavel = colaborador;
        return new Efeito("APROVAÇÃO", "Disponibilidade, persona, cota e valores conferidos.", usoDeCota());
    }

    public Efeito recusar(String motivo, long colaborador) {
        if (situacao != SituacaoReserva.PENDENTE) {
            throw new RegraException("Somente reservas pendentes podem ser recusadas.");
        }
        if (motivo == null || motivo.isBlank() || motivo.length() > MAXIMO_TEXTO) {
            throw new RegraException("Informe um motivo de recusa de até 2000 caracteres.");
        }
        situacao = SituacaoReserva.RECUSADA;
        motivoRecusa = motivo;
        responsavel = colaborador;
        return new Efeito("RECUSA", motivo, List.of());
    }

    /**
     * Cancela uma pendente ou aprovada. Aprovada que usava cota devolve as horas; cancelada com menos de 30 dias, sem
     * força maior, paga o dobro das horas no mês atual. Evento já terminado só sai por força maior do gestor.
     */
    public Efeito cancelar(boolean forcaMaior, String justificativa, boolean gestor, long colaborador,
            LocalDateTime agora) {
        exigirAtiva();
        if (forcaMaior && (!gestor || justificativa == null || justificativa.isBlank())) {
            throw new RegraException("A exceção exige um gestor e justificativa.");
        }
        if (situacao == SituacaoReserva.APROVADA && !forcaMaior && !fim().isAfter(agora)) {
            throw new RegraException("Este evento já terminou e não pode ser cancelado. Use Concluir ou registre uma"
                    + " exceção de força maior com justificativa.");
        }
        List<Movimento> movimentos = new ArrayList<>(devolucaoDeCota());
        boolean penalizar = situacao == SituacaoReserva.APROVADA && orcamento.usaCota() && !forcaMaior
                && ChronoUnit.DAYS
                        .between(agora.toLocalDate(), pedido.data()) < DIAS_SEM_PENALIDADE;
        BigDecimal penalidade = orcamento.horasCota().multiply(BigDecimal.valueOf(2));
        if (penalizar) {
            movimentos.add(new Movimento(TipoMovimento.PENALIDADE, YearMonth.from(agora), penalidade,
                    "Cancelamento fora do prazo"));
        }
        situacao = SituacaoReserva.CANCELADA;
        responsavel = colaborador;
        String texto = (penalizar ? "Penalidade: " + penalidade + "h. " : "Sem penalidade. ") + (justificativa == null
                ? ""
                : justificativa);
        return new Efeito("CANCELAMENTO", texto, List.copyOf(movimentos));
    }

    /**
     * Troca dia, horário, espaço ou persona. {@code novo} deve ter sido calculado com {@link #creditoDeCota()}, porque
     * as horas desta reserva voltam para a cota antes de usar as novas.
     */
    public Efeito alterar(Pedido novoPedido, Orcamento novo, long colaborador) {
        exigirAtiva();
        if (pagamento != SituacaoPagamento.PENDENTE) {
            throw new RegraException("Regularize o registro financeiro antes de alterar uma reserva com pagamento"
                    + " registrado.");
        }
        if (situacao == SituacaoReserva.APROVADA && !novo.taxasConfiguradas()) {
            throw new RegraException("Configure as taxas antes de alterar uma reserva aprovada.");
        }
        String antes = resumo(pedido);
        List<Movimento> movimentos = new ArrayList<>(devolucaoDeCota());
        pedido = novoPedido;
        orcamento = novo;
        movimentos.addAll(usoDeCota());
        responsavel = colaborador;
        return new Efeito("ALTERAÇÃO", antes + " → " + resumo(pedido), List.copyOf(movimentos));
    }

    public Efeito concluir(long colaborador, LocalDateTime agora) {
        if (situacao != SituacaoReserva.APROVADA || fim().isAfter(agora)) {
            throw new RegraException("Só é possível concluir uma reserva aprovada após seu término.");
        }
        situacao = SituacaoReserva.CONCLUIDA;
        responsavel = colaborador;
        return new Efeito("CONCLUSÃO", "Evento concluído.", List.of());
    }

    public Efeito registrarPagamento(SituacaoPagamento novo, long colaborador) {
        if (situacao != SituacaoReserva.APROVADA && situacao != SituacaoReserva.CONCLUIDA) {
            throw new RegraException("Registre pagamentos somente em reservas aprovadas ou concluídas.");
        }
        pagamento = Objects.requireNonNull(novo);
        return new Efeito("PAGAMENTO", novo.name(), List.of());
    }

    /** Pendente que passou do prazo vira cancelada. Devolve se mudou. */
    public boolean expirar(LocalDateTime agora) {
        if (situacao != SituacaoReserva.PENDENTE || expiraEm == null || expiraEm.isAfter(agora)) {
            return false;
        }
        situacao = SituacaoReserva.CANCELADA;
        return true;
    }

    /** Horas que esta reserva devolve à cota se for alterada (só aprovada que usa cota). */
    public BigDecimal creditoDeCota() {
        return situacao == SituacaoReserva.APROVADA ? orcamento.horasCota() : BigDecimal.ZERO;
    }

    public Intervalo intervalo() {
        return new Intervalo(pedido.inicio(), pedido.fim());
    }

    private List<Movimento> usoDeCota() {
        if (situacao != SituacaoReserva.APROVADA || !orcamento.usaCota()) {
            return List.of();
        }
        return List.of(new Movimento(TipoMovimento.UTILIZACAO, YearMonth.from(pedido.data()), orcamento.horasCota(),
                "Alocação da reserva"));
    }

    private List<Movimento> devolucaoDeCota() {
        if (situacao != SituacaoReserva.APROVADA || !orcamento.usaCota()) {
            return List.of();
        }
        return List.of(new Movimento(TipoMovimento.ESTORNO_UTILIZACAO, YearMonth.from(pedido.data()), orcamento
                .horasCota().negate(), "Liberação da reserva"));
    }

    private void exigirAtiva() {
        if (situacao != SituacaoReserva.PENDENTE && situacao != SituacaoReserva.APROVADA) {
            throw new RegraException("Esta reserva não permite mais esta ação.");
        }
    }

    private LocalDateTime fim() {
        return LocalDateTime.of(pedido.data(), pedido.fim());
    }

    private static String resumo(Pedido p) {
        return p.data() + " " + p.inicio() + "–" + p.fim() + " / " + p.persona().codigo();
    }

    public Pedido pedido() {
        return pedido;
    }

    public Orcamento orcamento() {
        return orcamento;
    }

    public SituacaoReserva situacao() {
        return situacao;
    }

    public SituacaoPagamento pagamento() {
        return pagamento;
    }

    public LocalDateTime solicitadaEm() {
        return solicitadaEm;
    }

    public LocalDateTime expiraEm() {
        return expiraEm;
    }

    public String motivoRecusa() {
        return motivoRecusa;
    }

    public Long responsavel() {
        return responsavel;
    }

    /** Dia do pedido, para a regra dos 15 dias da cota. */
    public LocalDate diaDoPedido() {
        return solicitadaEm.toLocalDate();
    }
}

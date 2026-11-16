package br.com.inpetu.hub.reserva;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import br.com.inpetu.hub.auditoria.Auditoria;
import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.RegraException;
import br.com.inpetu.hub.comum.Transacao;
import br.com.inpetu.hub.comum.log.Eventos;
import br.com.inpetu.hub.espaco.Espaco;
import br.com.inpetu.hub.espaco.EspacoRepositorio;

/**
 * As ações da Área Interna sobre uma reserva, cada uma numa {@link Transacao} (uma de cada vez): as regras ficam na
 * {@link Reserva}; aqui se confere a agenda, recalcula o preço e grava a reserva, o histórico, os movimentos de cota e
 * a auditoria.
 */
public final class ReservaServico {

    static final String ALVO = "reservas";

    private interface Acao {

        Reserva.Efeito aplicar(Connection conexao, ReservaRepositorio.Gravada gravada, LocalDateTime agora)
                throws SQLException;
    }

    private final Ambiente ambiente;
    private final DadosDoBanco dados;
    private final EspacoRepositorio espacos;
    private final ReservaRepositorio repositorio = new ReservaRepositorio();
    private final Auditoria auditoria;

    public ReservaServico(Ambiente ambiente) {
        this.ambiente = ambiente;
        this.dados = new DadosDoBanco(ambiente.banco());
        this.espacos = new EspacoRepositorio(ambiente.banco());
        this.auditoria = new Auditoria(ambiente.banco(), ambiente.relogio());
    }

    /**
     * Grava uma reserva pendente nova (o aceite de um pedido), dentro da transação de quem chama. Devolve o número.
     */
    public long criar(Connection conexao, Reserva reserva, long empreendedorId, long colaborador) throws SQLException {
        long id = repositorio.inserir(conexao, reserva, empreendedorId);
        repositorio.historico(conexao, id, LocalDateTime.now(ambiente.relogio()), "SOLICITAÇÃO",
                "Solicitação conferida; aguardando aprovação, sem bloqueio nem cota provisória.", colaborador);
        return id;
    }

    public void aprovar(long id, long colaborador) {
        agir(id, colaborador, "RESERVA_APROVADA", (conexao, g, agora) -> {
            Reserva r = g.reserva();
            Pedido p = r.pedido();
            Configuracao configuracao = dados.configuracao();
            Horarios.validar(espaco(p.espacoId()), p.data(), p.inicio(), p.fim(), p.participantes(), configuracao,
                    dados.aprovadas(p.espacoId(), p.data(), id), ambiente.relogio());
            return r.aprovar(orcamento(p, BigDecimal.ZERO, r.diaDoPedido(), configuracao), colaborador, agora);
        });
    }

    public void recusar(long id, String motivo, long colaborador) {
        agir(id, colaborador, "RESERVA_RECUSADA", (conexao, g, agora) -> g.reserva().recusar(motivo, colaborador));
    }

    public void cancelar(long id, boolean forcaMaior, String justificativa, long colaborador, boolean gestor) {
        if (justificativa != null && justificativa.length() > 2000) {
            throw new RegraException("Use uma justificativa de até 2000 caracteres.");
        }
        agir(id, colaborador, "RESERVA_CANCELADA", (conexao, g, agora) -> g.reserva().cancelar(forcaMaior,
                justificativa, gestor, colaborador, agora));
    }

    public void concluir(long id, long colaborador) {
        agir(id, colaborador, "RESERVA_CONCLUIDA", (conexao, g, agora) -> g.reserva().concluir(colaborador, agora));
    }

    public void registrarPagamento(long id, SituacaoPagamento situacao, long colaborador) {
        agir(id, colaborador, "PAGAMENTO_REGISTRADO", (conexao, g, agora) -> g.reserva().registrarPagamento(situacao,
                colaborador));
    }

    /** Troca dia, horário, espaço ou persona, recalculando valores e cota. */
    public void alterar(long id, LocalDate data, LocalTime inicio, LocalTime fim, long espacoId, long personaId,
            long colaborador) {
        agir(id, colaborador, "RESERVA_ALTERADA", (conexao, g, agora) -> {
            Reserva r = g.reserva();
            Pedido antes = r.pedido();
            Persona persona = dados.persona(personaId)
                    .orElseThrow(() -> new RegraException("Selecione uma persona da lista."))
                    .persona();
            Pedido novo = new Pedido(persona, espacoId, data, inicio, fim, antes.participantes(), antes
                    .inscricaoPaga(), antes.financiado(), antes.academico(), antes.titulo(), antes.objetivo(),
                    antes
                            .observacoes());
            Configuracao configuracao = dados.configuracao();
            Horarios.validar(espaco(espacoId), data, inicio, fim, novo.participantes(), configuracao, dados.aprovadas(
                    espacoId, data, id), ambiente.relogio());
            // As horas desta reserva só voltam para a mesma cota (mesma persona e mesmo mês).
            boolean mesmaCota = antes.persona().id() == personaId && YearMonth.from(antes.data()).equals(YearMonth.from(
                    data));
            Orcamento orcamento = orcamento(novo, mesmaCota ? r.creditoDeCota() : BigDecimal.ZERO, r.diaDoPedido(),
                    configuracao);
            String textoAntes = resumo(antes);
            Reserva.Efeito efeito = r.alterar(novo, orcamento, colaborador);
            return new Reserva.Efeito(efeito.acao(), textoAntes + " → " + resumo(novo), efeito.movimentos());
        });
    }

    /** Pendentes que passaram de 24 h viram canceladas. Devolve quantas. */
    public int expirarPendentes() {
        LocalDateTime agora = LocalDateTime.now(ambiente.relogio());
        int quantas = Transacao.executar(ambiente.banco(), conexao -> {
            int vencidas = 0;
            int turno = dados.configuracao().horasPorTurno();
            for (long id : repositorio.pendentesVencidas(conexao, agora)) {
                ReservaRepositorio.Gravada g = repositorio.carregar(conexao, id, turno).orElseThrow();
                if (g.reserva().expirar(agora)) {
                    repositorio.atualizar(conexao, id, g.reserva());
                    repositorio.historico(conexao, id, agora, "EXPIRAÇÃO", "Pendente sem análise em 24 h.", null);
                    auditoria.gravar(conexao, null, "PENDENTE_EXPIRADA", ALVO, id, "OK", null);
                    vencidas++;
                }
            }
            return vencidas;
        });
        for (int i = 0; i < quantas; i++) {
            Eventos.auditoria("PENDENTE_EXPIRADA", ALVO, "OK");
        }
        return quantas;
    }

    private void agir(long id, long colaborador, String evento, Acao acao) {
        Transacao.executar(ambiente.banco(), conexao -> {
            LocalDateTime agora = LocalDateTime.now(ambiente.relogio());
            ReservaRepositorio.Gravada g = repositorio.carregar(conexao, id, dados.configuracao().horasPorTurno())
                    .orElseThrow(() -> new RegraException("Reserva não encontrada."));
            long personaAntes = g.reserva().pedido().persona().id();
            Reserva.Efeito efeito = acao.aplicar(conexao, g, agora);
            repositorio.atualizar(conexao, id, g.reserva());
            long personaDepois = g.reserva().pedido().persona().id();
            for (Movimento m : efeito.movimentos()) {
                // A devolução de uma alteração vai para a persona de antes; o resto, para a de agora.
                long persona = m.tipo() == TipoMovimento.ESTORNO_UTILIZACAO ? personaAntes : personaDepois;
                repositorio.movimentar(conexao, persona, id, agora, m);
            }
            repositorio.historico(conexao, id, agora, efeito.acao(), efeito.descricao(), colaborador);
            auditoria.gravar(conexao, colaborador, evento, ALVO, id, "OK", null);
            return null;
        });
        Eventos.auditoria(evento, ALVO, "OK");
    }

    private Orcamento orcamento(Pedido pedido, BigDecimal credito, LocalDate diaDoPedido, Configuracao configuracao) {
        List<Movimento> movimentos = dados.movimentos(pedido.persona().id());
        CotaDoMes cota = Cota.doMes(pedido.persona(), movimentos, YearMonth.from(pedido.data()));
        return Orcamentos.calcular(pedido, cota, credito, diaDoPedido, configuracao, dados.precos(), dados.feriados());
    }

    private Espaco espaco(long id) {
        return espacos.porId(id).orElse(null);
    }

    private String resumo(Pedido p) {
        String nome = espacos.porId(p.espacoId()).map(Espaco::nome).orElse("espaço " + p.espacoId());
        return p.data() + " " + p.inicio() + "–" + p.fim() + " / " + nome + " / " + p.persona().codigo();
    }
}

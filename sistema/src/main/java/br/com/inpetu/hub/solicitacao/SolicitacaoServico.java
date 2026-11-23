package br.com.inpetu.hub.solicitacao;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import br.com.inpetu.hub.auditoria.Auditoria;
import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.RegraException;
import br.com.inpetu.hub.comum.Transacao;
import br.com.inpetu.hub.comum.log.Eventos;
import br.com.inpetu.hub.empreendedor.EmpreendedorRepositorio;
import br.com.inpetu.hub.espaco.Espaco;
import br.com.inpetu.hub.espaco.EspacoRepositorio;
import br.com.inpetu.hub.reserva.Cota;
import br.com.inpetu.hub.reserva.CotaDoMes;
import br.com.inpetu.hub.reserva.DadosDoBanco;
import br.com.inpetu.hub.reserva.Horarios;
import br.com.inpetu.hub.reserva.Orcamento;
import br.com.inpetu.hub.reserva.Orcamentos;
import br.com.inpetu.hub.reserva.Pedido;
import br.com.inpetu.hub.reserva.Reserva;
import br.com.inpetu.hub.reserva.ReservaServico;

/**
 * O pedido público: estima (horário e preço, sem gravar) e registra. O registro só grava o pedido, para a equipe
 * conferir (decisão 13: confirmação manual): não cria reserva, não usa cota e não cria cadastro de empreendedor. A
 * mesma chave nunca grava duas vezes. Pedidos não analisados vencem em 24 h.
 */
public final class SolicitacaoServico {

    static final String ALVO = "solicitacao";
    private static final int PRAZO_HORAS = 24;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final Ambiente ambiente;
    private final DadosDoBanco dados;
    private final EspacoRepositorio espacos;
    private final Auditoria auditoria;
    private final EmpreendedorRepositorio empreendedores = new EmpreendedorRepositorio();
    private final ReservaServico reservas;

    private static final String COLUNAS_PEDIDO = "s.espaco_id, s.persona_id, s.data, s.hora_inicio, s.hora_fim,"
            + " s.numero_participantes, s.inscricao_paga, s.financiado, s.academico, s.titulo, s.objetivo,"
            + " s.observacoes, s.nome, s.tipo_documento, s.documento, s.email, s.telefone, s.empresa, s.cidade,"
            + " s.segmento, s.cargo";

    public SolicitacaoServico(Ambiente ambiente) {
        this.ambiente = ambiente;
        this.dados = new DadosDoBanco(ambiente.banco());
        this.espacos = new EspacoRepositorio(ambiente.banco());
        this.auditoria = new Auditoria(ambiente.banco(), ambiente.relogio());
        this.reservas = new ReservaServico(ambiente);
    }

    /** Confere o horário e calcula o orçamento. Recusa com {@link RegraException} e a mensagem para a pessoa. */
    public Estimativa estimar(PedidoConferido p) {
        Espaco espaco = espacos.porId(p.espacoId())
                .orElseThrow(() -> new RegraException("Selecione um espaço da lista."));
        DadosDoBanco.PersonaComNome persona = dados.persona(p.personaId())
                .orElseThrow(() -> new RegraException("Selecione uma persona da lista."));
        var configuracao = dados.configuracao();
        Horarios.validar(espaco, p.data(), p.inicio(), p.fim(), p.participantes(), configuracao, dados.aprovadas(
                espaco.id(), p.data(), 0), ambiente.relogio());
        Pedido pedido = new Pedido(persona.persona(), espaco.id(), p.data(), p.inicio(), p.fim(), p.participantes(), p
                .inscricaoPaga(), p.financiado(), p.academico(), p.titulo(), p.objetivo(), p.observacoes());
        CotaDoMes cota = Cota.doMes(persona.persona(), dados.movimentos(persona.persona().id()), YearMonth.from(p
                .data()));
        Orcamento orcamento = Orcamentos.calcular(pedido, cota, BigDecimal.ZERO, LocalDate.now(ambiente.relogio()),
                configuracao, dados.precos(), dados.feriados());
        return new Estimativa(pedido, espaco, persona, orcamento, cota);
    }

    /** Grava o pedido (conferido de novo) e devolve a referência. A mesma chave devolve o pedido já gravado. */
    public String registrar(PedidoConferido p, String chave) {
        if (chave == null || !chave.matches("[A-Za-z0-9-]{16,64}")) {
            throw new RegraException("Reabra o formulário antes de confirmar.");
        }
        Optional<String> existente = referenciaPelaChave(chave);
        if (existente.isPresent()) {
            return existente.get();
        }
        estimar(p);
        String referencia = referenciaNova();
        LocalDateTime agora = LocalDateTime.now(ambiente.relogio());
        long id;
        try {
            id = inserir(p, chave, referencia, agora);
        } catch (SQLIntegrityConstraintViolationException mesmaChave) {
            // Dois envios da mesma chave ao mesmo tempo: vale o que gravou primeiro.
            return referenciaPelaChave(chave).orElseThrow(() -> new IllegalStateException(mesmaChave));
        } catch (SQLException falha) {
            throw new IllegalStateException("Não foi possível gravar o pedido.", falha);
        }
        auditoria.registrar(null, "SOLICITACAO_RECEBIDA", ALVO, id, "OK",
                "Sem bloqueio de horário e sem alocação de cotas.");
        return referencia;
    }

    /**
     * Aceita o pedido depois da conferência manual (decisão 13): liga ao cadastro do empreendedor (pelo documento ou
     * pelo email) e cria a reserva PENDENTE, sem ocupar horário nem cota. Devolve o número da reserva.
     */
    public long aceitar(long pedidoId, long colaborador, boolean identidadeConferida, String comoConferiu) {
        if (!identidadeConferida || comoConferiu == null || comoConferiu.isBlank() || comoConferiu.length() > 2000) {
            throw new RegraException("Confira a identidade e informe como o vínculo com a instituição foi verificado.");
        }
        long reserva = Transacao.executar(ambiente.banco(), conexao -> {
            LocalDateTime agora = LocalDateTime.now(ambiente.relogio());
            PedidoConferido p = recebido(conexao, pedidoId, agora);
            Estimativa estimativa = estimar(p);
            long empreendedor = empreendedores.vincular(conexao, p);
            long id = reservas.criar(conexao, Reserva.solicitar(estimativa.pedido(), estimativa.orcamento(), agora),
                    empreendedor, colaborador);
            try (PreparedStatement sql = conexao.prepareStatement("update solicitacao_publica set estado ="
                    + " 'ENCAMINHADA', reserva_id = ?, justificativa = ?, codigo_hash = null where id = ?")) {
                sql.setLong(1, id);
                sql.setString(2, comoConferiu.strip());
                sql.setLong(3, pedidoId);
                sql.executeUpdate();
            }
            auditoria.gravar(conexao, colaborador, "IDENTIDADE_CONFERIDA", ALVO, pedidoId, "OK", null);
            return id;
        });
        Eventos.auditoria("IDENTIDADE_CONFERIDA", ALVO, "OK");
        return reserva;
    }

    /** Recusa um pedido ainda não analisado. */
    public void recusar(long pedidoId, long colaborador) {
        Transacao.executar(ambiente.banco(), conexao -> {
            try (PreparedStatement sql = conexao.prepareStatement("update solicitacao_publica set estado = 'RECUSADA',"
                    + " codigo_hash = null where id = ? and estado = 'RECEBIDA'")) {
                sql.setLong(1, pedidoId);
                if (sql.executeUpdate() != 1) {
                    throw new RegraException("Solicitação não encontrada ou já processada.");
                }
            }
            auditoria.gravar(conexao, colaborador, "SOLICITACAO_RECUSADA", ALVO, pedidoId, "OK", null);
            return null;
        });
        Eventos.auditoria("SOLICITACAO_RECUSADA", ALVO, "OK");
    }

    /** Pedidos ainda não analisados, do mais antigo para o mais novo (até 200). */
    public List<PedidoRecebido> recebidos() {
        List<PedidoRecebido> lista = new ArrayList<>();
        try (Connection conexao = ambiente.banco().getConnection();
                PreparedStatement sql = conexao.prepareStatement(
                        "select s.id, " + COLUNAS_PEDIDO + ", s.expira_em, e.nome_espaco, p.codigo, p.nome as persona"
                                + " from solicitacao_publica s left join espaco e on e.id_espaco = s.espaco_id"
                                + " left join persona p on p.id = s.persona_id where s.estado = 'RECEBIDA'"
                                + " order by s.criada_em, s.id limit 200");
                ResultSet linha = sql.executeQuery()) {
            while (linha.next()) {
                lista.add(new PedidoRecebido(linha.getLong("id"), pedido(linha), linha.getTimestamp("expira_em")
                        .toLocalDateTime(), linha.getString("nome_espaco"),
                        linha.getString("codigo") + " — " + linha
                                .getString("persona")));
            }
            return lista;
        } catch (SQLException falha) {
            throw new IllegalStateException("Não foi possível ler os pedidos.", falha);
        }
    }

    private PedidoConferido recebido(Connection conexao, long pedidoId, LocalDateTime agora) throws SQLException {
        try (PreparedStatement sql = conexao.prepareStatement("select s.id, " + COLUNAS_PEDIDO
                + ", s.estado, s.expira_em from solicitacao_publica s where s.id = ?")) {
            sql.setLong(1, pedidoId);
            try (ResultSet linha = sql.executeQuery()) {
                if (!linha.next()) {
                    throw new RegraException("Solicitação não encontrada.");
                }
                if (!"RECEBIDA".equals(linha.getString("estado")) || linha.getTimestamp("expira_em").toLocalDateTime()
                        .isBefore(agora)) {
                    throw new RegraException("Solicitação processada ou expirada.");
                }
                return pedido(linha);
            }
        }
    }

    private static PedidoConferido pedido(ResultSet l) throws SQLException {
        return new PedidoConferido(l.getLong("espaco_id"), l.getLong("persona_id"), l.getDate("data").toLocalDate(), l
                .getTime("hora_inicio").toLocalTime(), l.getTime("hora_fim").toLocalTime(),
                l.getInt(
                        "numero_participantes"),
                l.getBoolean("inscricao_paga"), l.getBoolean("financiado"), l
                        .getBoolean("academico"),
                l.getString("titulo"), l.getString("objetivo"), l.getString(
                        "observacoes"),
                l.getString("nome"), l.getString("tipo_documento"), l
                        .getString("documento"),
                l.getString("email"), l.getString(
                        "telefone"),
                l.getString("empresa"), l.getString("cidade"), l
                        .getString("segmento"),
                l.getString("cargo"));
    }

    /** Pedidos ainda não analisados que passaram de 24 h viram EXPIRADA. Devolve quantos. */
    public int expirarVencidas() {
        LocalDateTime agora = LocalDateTime.now(ambiente.relogio());
        List<Long> vencidas = new ArrayList<>();
        try (Connection conexao = ambiente.banco().getConnection()) {
            try (PreparedStatement sql = conexao.prepareStatement(
                    "select id from solicitacao_publica where estado = 'RECEBIDA' and expira_em <= ? order by id")) {
                sql.setTimestamp(1, Timestamp.valueOf(agora));
                try (ResultSet linha = sql.executeQuery()) {
                    while (linha.next()) {
                        vencidas.add(linha.getLong(1));
                    }
                }
            }
            int expiradas = 0;
            for (long id : vencidas) {
                try (PreparedStatement sql = conexao.prepareStatement("update solicitacao_publica set estado ="
                        + " 'EXPIRADA', codigo_hash = null where id = ? and estado = 'RECEBIDA'")) {
                    sql.setLong(1, id);
                    if (sql.executeUpdate() == 1) {
                        expiradas++;
                        auditoria.registrar(null, "SOLICITACAO_EXPIRADA", ALVO, id, "OK", null);
                    }
                }
            }
            return expiradas;
        } catch (SQLException falha) {
            throw new IllegalStateException("Não foi possível vencer os pedidos.", falha);
        }
    }

    private long inserir(PedidoConferido p, String chave, String referencia, LocalDateTime agora) throws SQLException {
        try (Connection conexao = ambiente.banco().getConnection();
                PreparedStatement sql = conexao.prepareStatement(
                        "insert into solicitacao_publica(referencia, chave, espaco_id, persona_id, data, hora_inicio, hora_fim,"
                                + " nome, documento, tipo_documento, email, telefone, empresa, cidade, segmento, cargo, titulo,"
                                + " objetivo, observacoes, numero_participantes, inscricao_paga, financiado, academico,"
                                + " criada_em, expira_em, email_confirmado, tentativas, estado)"
                                + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false, 0,"
                                + " 'RECEBIDA')",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
            sql.setString(1, referencia);
            sql.setString(2, chave);
            sql.setLong(3, p.espacoId());
            sql.setLong(4, p.personaId());
            sql.setDate(5, Date.valueOf(p.data()));
            sql.setTime(6, Time.valueOf(p.inicio()));
            sql.setTime(7, Time.valueOf(p.fim()));
            String[] textos = {p.nome(), p.documento(), p.tipoDocumento(), p.email(), p.telefone(), p.empresa(), p
                    .cidade(), p.segmento(), p.cargo(), p.titulo(), p.objetivo(), p.observacoes()};
            for (int i = 0; i < textos.length; i++) {
                if (textos[i] == null) {
                    sql.setNull(8 + i, Types.VARCHAR);
                } else {
                    sql.setString(8 + i, textos[i]);
                }
            }
            sql.setInt(20, p.participantes());
            sql.setBoolean(21, p.inscricaoPaga());
            sql.setBoolean(22, p.financiado());
            sql.setBoolean(23, p.academico());
            sql.setTimestamp(24, Timestamp.valueOf(agora));
            sql.setTimestamp(25, Timestamp.valueOf(agora.plusHours(PRAZO_HORAS)));
            sql.executeUpdate();
            try (ResultSet chaveGerada = sql.getGeneratedKeys()) {
                chaveGerada.next();
                return chaveGerada.getLong(1);
            }
        }
    }

    private Optional<String> referenciaPelaChave(String chave) {
        try (Connection conexao = ambiente.banco().getConnection();
                PreparedStatement sql = conexao.prepareStatement(
                        "select referencia from solicitacao_publica where chave = ?")) {
            sql.setString(1, chave);
            try (ResultSet linha = sql.executeQuery()) {
                return linha.next() ? Optional.of(linha.getString(1)) : Optional.empty();
            }
        } catch (SQLException falha) {
            throw new IllegalStateException("Não foi possível ler o pedido.", falha);
        }
    }

    private static String referenciaNova() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

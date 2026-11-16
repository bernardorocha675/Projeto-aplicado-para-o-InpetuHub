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
import br.com.inpetu.hub.espaco.Espaco;
import br.com.inpetu.hub.espaco.EspacoRepositorio;
import br.com.inpetu.hub.reserva.Cota;
import br.com.inpetu.hub.reserva.CotaDoMes;
import br.com.inpetu.hub.reserva.DadosDoBanco;
import br.com.inpetu.hub.reserva.Horarios;
import br.com.inpetu.hub.reserva.Orcamento;
import br.com.inpetu.hub.reserva.Orcamentos;
import br.com.inpetu.hub.reserva.Pedido;

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

    public SolicitacaoServico(Ambiente ambiente) {
        this.ambiente = ambiente;
        this.dados = new DadosDoBanco(ambiente.banco());
        this.espacos = new EspacoRepositorio(ambiente.banco());
        this.auditoria = new Auditoria(ambiente.banco(), ambiente.relogio());
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
        return new Estimativa(espaco, persona, orcamento, cota);
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

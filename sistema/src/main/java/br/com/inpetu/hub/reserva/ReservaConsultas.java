package br.com.inpetu.hub.reserva;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.sql.DataSource;

import br.com.inpetu.hub.empreendedor.Documentos;

/** Leituras das telas da Área Interna: lista com filtros, detalhe com histórico e os números do painel. */
public final class ReservaConsultas {

    /** Uma linha da lista de reservas. */
    public record Linha(long id, LocalDate data, LocalTime inicio, LocalTime fim, String solicitante, String empresa,
            String documento, String espaco, String persona, SituacaoReserva situacao) {
    }

    /** Filtros da lista (nulos ou vazios não filtram). */
    public record Filtro(SituacaoReserva situacao, LocalDate data, Long espacoId, Long personaId, String busca) {
    }

    /** Uma linha do histórico da reserva. */
    public record Evento(LocalDateTime quando, String acao, String descricao, String quem) {
    }

    /** Tudo o que o detalhe mostra. */
    public record Detalhe(Linha linha, long empreendedorId, String email, String telefone, String cidade,
            String segmento, String cargo, String titulo, String objetivo, String observacoes, int participantes,
            boolean inscricaoPaga, boolean financiado, boolean academico, long espacoId, long personaId,
            String personaNome, LocalDateTime solicitadaEm, LocalDateTime expiraEm, boolean usaCota,
            BigDecimal horasCota, int locacao, int operacao, int limpeza, BigDecimal valorBase,
            BigDecimal valorOperacao, BigDecimal valorLimpeza, BigDecimal valorFinal, int percentualSinal,
            BigDecimal valorSinal, BigDecimal valorRestante, boolean taxasConfiguradas, SituacaoPagamento pagamento,
            String responsavel, String motivoRecusa, CotaDoMes cota, List<Evento> historico) {
    }

    private static final String LINHA = "select r.id_reserva, r.data, r.hora_inicio, r.hora_fim, e.nome, e.empresa,"
            + " e.tipo_documento, e.documento, s.nome_espaco, p.codigo, r.status_reserva from reserva r"
            + " join empreendedor e on e.id_empreendedor = r.id_empreendedor"
            + " join espaco s on s.id_espaco = r.id_espaco join persona p on p.id = r.persona_id";

    private final DataSource banco;
    private final DadosDoBanco dados;

    public ReservaConsultas(DataSource banco) {
        this.banco = banco;
        this.dados = new DadosDoBanco(banco);
    }

    public List<Linha> listar(Filtro f) {
        StringBuilder consulta = new StringBuilder(LINHA).append(" where 1 = 1");
        List<Object> valores = new ArrayList<>();
        if (f.situacao() != null) {
            consulta.append(" and r.status_reserva = ?");
            valores.add(f.situacao().name());
        }
        if (f.data() != null) {
            consulta.append(" and r.data = ?");
            valores.add(Date.valueOf(f.data()));
        }
        if (f.espacoId() != null) {
            consulta.append(" and r.id_espaco = ?");
            valores.add(f.espacoId());
        }
        if (f.personaId() != null) {
            consulta.append(" and r.persona_id = ?");
            valores.add(f.personaId());
        }
        if (f.busca() != null && !f.busca().isBlank()) {
            consulta.append(" and lower(concat(coalesce(e.nome, ''), ' ', coalesce(e.empresa, ''), ' ',"
                    + " coalesce(e.documento, ''))) like ?");
            valores.add("%" + f.busca().strip().toLowerCase(Locale.ROOT).replace("%", "").replace("_", "") + "%");
        }
        consulta.append(" order by r.data, r.hora_inicio, r.id_reserva limit 500");
        return linhas(consulta.toString(), valores);
    }

    /** Pendentes e aprovadas de hoje em diante, da mais próxima. */
    public List<Linha> proximas(LocalDate hoje, int quantas) {
        return linhas(LINHA + " where r.data >= ? and r.status_reserva in ('PENDENTE', 'APROVADA')"
                + " order by r.data, r.hora_inicio, r.id_reserva limit " + quantas, List.of(Date.valueOf(hoje)));
    }

    public long contar(String consulta, Object... valores) {
        try (Connection conexao = banco.getConnection(); PreparedStatement sql = conexao.prepareStatement(consulta)) {
            for (int i = 0; i < valores.length; i++) {
                sql.setObject(i + 1, valores[i]);
            }
            try (ResultSet linha = sql.executeQuery()) {
                linha.next();
                return linha.getLong(1);
            }
        } catch (SQLException falha) {
            throw new IllegalStateException("Não foi possível contar.", falha);
        }
    }

    public Optional<Detalhe> detalhe(long id) {
        try (Connection conexao = banco.getConnection();
                PreparedStatement sql = conexao.prepareStatement(LINHA
                        .replace("select ",
                                "select r.id_empreendedor as emp, e.email, e.telefone, e.cidade, e.segmento,"
                                        + " e.cargo, r.titulo, r.objetivo, r.observacoes, r.numero_participantes, r.inscricao_paga,"
                                        + " r.financiado, r.academico, r.id_espaco as espaco_id, r.persona_id, p.nome as persona_nome,"
                                        + " p.cota_mensal_horas, r.data_hora_solicitacao, r.expira_em, r.usa_cota, r.horas_cota,"
                                        + " r.percentual_locacao, r.percentual_operacao, r.percentual_limpeza, r.valor_base,"
                                        + " r.valor_operacao, r.valor_limpeza, r.valor_final, r.percentual_sinal, r.valor_sinal,"
                                        + " r.valor_restante, r.taxas_configuradas, r.status_pagamento, r.motivo_recusa,"
                                        + " (select c.nome from colaborador c where c.id_colaborador = r.id_colaborador_responsavel)"
                                        + " as responsavel, ")
                        + " where r.id_reserva = ?")) {
            sql.setLong(1, id);
            try (ResultSet l = sql.executeQuery()) {
                if (!l.next()) {
                    return Optional.empty();
                }
                Linha linha = linha(l);
                Persona persona = new Persona(l.getLong("persona_id"), linha.persona(), l.getBigDecimal(
                        "cota_mensal_horas"));
                CotaDoMes cota = Cota.doMes(persona, dados.movimentos(persona.id()), YearMonth.from(linha.data()));
                return Optional.of(new Detalhe(linha, l.getLong("emp"), l.getString("email"), l.getString("telefone"), l
                        .getString("cidade"), l.getString("segmento"), l.getString("cargo"), l.getString("titulo"),
                        l
                                .getString("objetivo"),
                        l.getString("observacoes"), l.getInt("numero_participantes"), l
                                .getBoolean("inscricao_paga"),
                        l.getBoolean("financiado"), l.getBoolean(
                                "academico"),
                        l.getLong("espaco_id"), persona.id(), l.getString(
                                "persona_nome"),
                        dataHora(l, "data_hora_solicitacao"),
                        dataHora(l, "expira_em"), l.getBoolean("usa_cota"), l.getBigDecimal("horas_cota"), l.getInt(
                                "percentual_locacao"),
                        l.getInt("percentual_operacao"), l.getInt("percentual_limpeza"),
                        l.getBigDecimal("valor_base"), l.getBigDecimal("valor_operacao"), l.getBigDecimal(
                                "valor_limpeza"),
                        l.getBigDecimal("valor_final"), l.getInt("percentual_sinal"), l
                                .getBigDecimal("valor_sinal"),
                        l.getBigDecimal("valor_restante"), l.getBoolean(
                                "taxas_configuradas"),
                        SituacaoPagamento.valueOf(l.getString(
                                "status_pagamento")),
                        l.getString("responsavel"), l.getString(
                                "motivo_recusa"),
                        cota, historico(conexao, id)));
            }
        } catch (SQLException falha) {
            throw new IllegalStateException("Não foi possível ler a reserva.", falha);
        }
    }

    private static List<Evento> historico(Connection conexao, long id) throws SQLException {
        List<Evento> eventos = new ArrayList<>();
        try (PreparedStatement sql = conexao.prepareStatement("select h.data_hora, h.acao, h.descricao, c.nome from"
                + " historico_reserva h left join colaborador c on c.id_colaborador = h.id_colaborador"
                + " where h.reserva_id = ? order by h.data_hora desc, h.id desc")) {
            sql.setLong(1, id);
            try (ResultSet l = sql.executeQuery()) {
                while (l.next()) {
                    eventos.add(new Evento(l.getTimestamp(1).toLocalDateTime(), l.getString(2), l.getString(3), l
                            .getString(4) == null ? "Sistema" : l.getString(4)));
                }
            }
        }
        return eventos;
    }

    private List<Linha> linhas(String consulta, List<Object> valores) {
        List<Linha> lista = new ArrayList<>();
        try (Connection conexao = banco.getConnection(); PreparedStatement sql = conexao.prepareStatement(consulta)) {
            for (int i = 0; i < valores.size(); i++) {
                sql.setObject(i + 1, valores.get(i));
            }
            try (ResultSet l = sql.executeQuery()) {
                while (l.next()) {
                    lista.add(linha(l));
                }
            }
            return lista;
        } catch (SQLException falha) {
            throw new IllegalStateException("Não foi possível ler as reservas.", falha);
        }
    }

    private static Linha linha(ResultSet l) throws SQLException {
        return new Linha(l.getLong("id_reserva"), l.getDate("data").toLocalDate(), l.getTime("hora_inicio")
                .toLocalTime(), l.getTime("hora_fim").toLocalTime(), l.getString("nome"), l.getString("empresa"),
                Documentos.exibir(l.getString("tipo_documento"), l.getString("documento")), l.getString("nome_espaco"),
                l.getString("codigo"), SituacaoReserva.valueOf(l.getString("status_reserva")));
    }

    private static LocalDateTime dataHora(ResultSet l, String coluna) throws SQLException {
        java.sql.Timestamp valor = l.getTimestamp(coluna);
        return valor == null ? null : valor.toLocalDateTime();
    }
}

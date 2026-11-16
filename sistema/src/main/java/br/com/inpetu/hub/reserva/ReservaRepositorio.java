package br.com.inpetu.hub.reserva;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Grava e lê a reserva, o histórico e os movimentos de cota, sempre na conexão da transação. */
final class ReservaRepositorio {

    /** Uma reserva gravada: o número, a reserva e o cadastro do solicitante. */
    record Gravada(long id, Reserva reserva, long empreendedorId) {
    }

    private static final String COLUNAS = "id_espaco, persona_id, id_colaborador_responsavel, data, hora_inicio,"
            + " hora_fim, data_hora_solicitacao, expira_em, status_reserva, status_pagamento, titulo, objetivo,"
            + " observacoes, motivo_recusa, numero_participantes, inscricao_paga, financiado, academico, usa_cota,"
            + " horas_cota, valor_base, valor_final, valor_sinal, valor_restante, percentual_sinal,"
            + " percentual_locacao, percentual_operacao, percentual_limpeza, valor_operacao, valor_limpeza,"
            + " taxas_configuradas";

    long inserir(Connection conexao, Reserva reserva, long empreendedorId) throws SQLException {
        try (PreparedStatement sql = conexao.prepareStatement("insert into reserva(id_empreendedor, " + COLUNAS
                + ") values (?" + ", ?".repeat(31) + ")", Statement.RETURN_GENERATED_KEYS)) {
            sql.setLong(1, empreendedorId);
            preencher(sql, 2, reserva);
            sql.executeUpdate();
            try (ResultSet chave = sql.getGeneratedKeys()) {
                chave.next();
                return chave.getLong(1);
            }
        }
    }

    void atualizar(Connection conexao, long id, Reserva reserva) throws SQLException {
        String atribuicoes = String.join(" = ?, ", COLUNAS.split(", ")) + " = ?";
        try (PreparedStatement sql = conexao.prepareStatement("update reserva set " + atribuicoes
                + " where id_reserva = ?")) {
            int proximo = preencher(sql, 1, reserva);
            sql.setLong(proximo, id);
            sql.executeUpdate();
        }
    }

    Optional<Gravada> carregar(Connection conexao, long id, int horasPorTurno) throws SQLException {
        try (PreparedStatement sql = conexao.prepareStatement("select r.id_empreendedor, " + prefixar(COLUNAS)
                + ", p.codigo, p.cota_mensal_horas from reserva r join persona p on p.id = r.persona_id"
                + " where r.id_reserva = ?")) {
            sql.setLong(1, id);
            try (ResultSet l = sql.executeQuery()) {
                if (!l.next()) {
                    return Optional.empty();
                }
                Persona persona = new Persona(l.getLong("persona_id"), l.getString("codigo"), l.getBigDecimal(
                        "cota_mensal_horas"));
                Pedido pedido = new Pedido(persona, l.getLong("id_espaco"), l.getDate("data").toLocalDate(), l.getTime(
                        "hora_inicio").toLocalTime(), l.getTime("hora_fim").toLocalTime(), l.getInt(
                                "numero_participantes"),
                        l.getBoolean("inscricao_paga"), l.getBoolean("financiado"), l
                                .getBoolean("academico"),
                        l.getString("titulo"), l.getString("objetivo"), l
                                .getString("observacoes"));
                BigDecimal horas = BigDecimal.valueOf(Duration.between(pedido.inicio(), pedido.fim()).toMinutes())
                        .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
                int turnos = horas.divide(BigDecimal.valueOf(horasPorTurno), 0, RoundingMode.CEILING).intValue();
                boolean usaCota = l.getBoolean("usa_cota");
                Orcamento orcamento = new Orcamento(horas, turnos, usaCota, usaCota, l.getInt("percentual_locacao"), l
                        .getInt("percentual_operacao"), l.getInt("percentual_limpeza"), l.getBigDecimal("valor_base"),
                        l
                                .getBigDecimal("valor_operacao"),
                        l.getBigDecimal("valor_limpeza"), l.getBigDecimal(
                                "valor_final"),
                        l.getInt("percentual_sinal"), l.getBigDecimal("valor_sinal"), l
                                .getBigDecimal("valor_restante"),
                        l.getBoolean("taxas_configuradas"));
                Object responsavel = l.getObject("id_colaborador_responsavel");
                Reserva reserva = Reserva.restaurar(pedido, orcamento, SituacaoReserva.valueOf(l.getString(
                        "status_reserva")), SituacaoPagamento.valueOf(l.getString("status_pagamento")), dataHora(
                                l
                                        .getTimestamp("data_hora_solicitacao")),
                        dataHora(l.getTimestamp("expira_em")), l
                                .getString("motivo_recusa"),
                        responsavel == null
                                ? null
                                : ((Number) responsavel).longValue());
                return Optional.of(new Gravada(id, reserva, l.getLong("id_empreendedor")));
            }
        }
    }

    void movimentar(Connection conexao, long personaId, long reservaId, LocalDateTime agora, Movimento movimento)
            throws SQLException {
        try (PreparedStatement sql = conexao.prepareStatement("insert into movimentacao_cota(persona_id, reserva_id,"
                + " data_hora, mes_referencia, quantidade_horas, tipo, descricao) values (?, ?, ?, ?, ?, ?, ?)")) {
            sql.setLong(1, personaId);
            sql.setLong(2, reservaId);
            sql.setTimestamp(3, Timestamp.valueOf(agora));
            sql.setDate(4, Date.valueOf(movimento.mes().atDay(1)));
            sql.setBigDecimal(5, movimento.horas());
            sql.setString(6, movimento.tipo().name());
            sql.setString(7, movimento.descricao() + " #" + reservaId);
            sql.executeUpdate();
        }
    }

    void historico(Connection conexao, long reservaId, LocalDateTime agora, String acao, String descricao,
            Long colaborador) throws SQLException {
        try (PreparedStatement sql = conexao.prepareStatement("insert into historico_reserva(reserva_id, data_hora,"
                + " acao, descricao, id_colaborador) values (?, ?, ?, ?, ?)")) {
            sql.setLong(1, reservaId);
            sql.setTimestamp(2, Timestamp.valueOf(agora));
            sql.setString(3, acao);
            sql.setString(4, descricao);
            if (colaborador == null) {
                sql.setNull(5, Types.BIGINT);
            } else {
                sql.setLong(5, colaborador);
            }
            sql.executeUpdate();
        }
    }

    List<Long> pendentesVencidas(Connection conexao, LocalDateTime agora) throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement sql = conexao.prepareStatement("select id_reserva from reserva where status_reserva ="
                + " 'PENDENTE' and expira_em <= ? order by id_reserva")) {
            sql.setTimestamp(1, Timestamp.valueOf(agora));
            try (ResultSet linha = sql.executeQuery()) {
                while (linha.next()) {
                    ids.add(linha.getLong(1));
                }
            }
        }
        return ids;
    }

    private static int preencher(PreparedStatement sql, int inicio, Reserva reserva) throws SQLException {
        Pedido p = reserva.pedido();
        Orcamento o = reserva.orcamento();
        int i = inicio;
        sql.setLong(i++, p.espacoId());
        sql.setLong(i++, p.persona().id());
        if (reserva.responsavel() == null) {
            sql.setNull(i++, Types.BIGINT);
        } else {
            sql.setLong(i++, reserva.responsavel());
        }
        sql.setDate(i++, Date.valueOf(p.data()));
        sql.setTime(i++, Time.valueOf(p.inicio()));
        sql.setTime(i++, Time.valueOf(p.fim()));
        sql.setTimestamp(i++, reserva.solicitadaEm() == null ? null : Timestamp.valueOf(reserva.solicitadaEm()));
        sql.setTimestamp(i++, reserva.expiraEm() == null ? null : Timestamp.valueOf(reserva.expiraEm()));
        sql.setString(i++, reserva.situacao().name());
        sql.setString(i++, reserva.pagamento().name());
        sql.setString(i++, p.titulo());
        sql.setString(i++, p.objetivo());
        sql.setString(i++, p.observacoes());
        sql.setString(i++, reserva.motivoRecusa());
        sql.setInt(i++, p.participantes());
        sql.setBoolean(i++, p.inscricaoPaga());
        sql.setBoolean(i++, p.financiado());
        sql.setBoolean(i++, p.academico());
        sql.setBoolean(i++, o.usaCota());
        sql.setBigDecimal(i++, o.horasCota());
        sql.setBigDecimal(i++, o.valorBase());
        sql.setBigDecimal(i++, o.valorFinal());
        sql.setBigDecimal(i++, o.valorSinal());
        sql.setBigDecimal(i++, o.valorRestante());
        sql.setInt(i++, o.percentualSinal());
        sql.setInt(i++, o.locacao());
        sql.setInt(i++, o.operacao());
        sql.setInt(i++, o.limpeza());
        sql.setBigDecimal(i++, o.valorOperacao());
        sql.setBigDecimal(i++, o.valorLimpeza());
        sql.setBoolean(i++, o.taxasConfiguradas());
        return i;
    }

    private static String prefixar(String colunas) {
        return "r." + String.join(", r.", colunas.split(", "));
    }

    private static LocalDateTime dataHora(Timestamp valor) {
        return valor == null ? null : valor.toLocalDateTime();
    }
}

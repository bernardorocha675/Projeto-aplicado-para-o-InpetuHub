package br.com.inpetu.hub.comum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.sql.DataSource;

/**
 * O que o banco novo precisa ter depois das migrations, conferido igual no H2 e no MySQL.
 */
final class ConferenciaDoModelo {

    static final Set<String> TABELAS = Set.of("empreendedor", "espaco", "colaborador", "persona", "configuracao",
            "feriado", "tabela_preco", "reserva", "atendimento", "visita", "cota_mensal", "movimentacao_cota",
            "historico_reserva", "auditoria", "solicitacao_publica", "bootstrap_seguro");

    private ConferenciaDoModelo() {
    }

    static void conferirTudo(DataSource banco) throws SQLException {
        assertEquals(1, Migracoes.aplicar(banco), "a primeira subida aplica a V1");
        assertEquals(0, Migracoes.aplicar(banco), "a segunda subida não aplica nada de novo");
        try (Connection conexao = banco.getConnection()) {
            conferirTabelas(conexao);
            conferirDocumento(conexao);
            conferirPersonas(conexao);
            conferirConfiguracao(conexao);
            conferirPrecos(conexao);
            conferirFeriados(conexao);
            conferirEspacos(conexao);
            conferirRegrasDoBanco(conexao);
        }
    }

    private static void conferirTabelas(Connection conexao) throws SQLException {
        Set<String> tabelas = new TreeSet<>();
        DatabaseMetaData meta = conexao.getMetaData();
        try (ResultSet rs = meta.getTables(conexao.getCatalog(), conexao.getSchema(), "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String nome = rs.getString("TABLE_NAME").toLowerCase(Locale.ROOT);
                if (!nome.startsWith("flyway_")) {
                    tabelas.add(nome);
                }
            }
        }
        assertEquals(new TreeSet<>(TABELAS), tabelas, "tabelas do modelo (sem as de importação)");
        Set<String> colaborador = colunas(conexao, "colaborador");
        assertTrue(colaborador.contains("security_version"));
        assertTrue(colaborador.stream().noneMatch(c -> c.startsWith("mfa")), "sem colunas de MFA: " + colaborador);
        assertTrue(colunas(conexao, "espaco").contains("mobiliario"));
        assertTrue(colunas(conexao, "reserva").contains("expira_em"));
    }

    private static void conferirDocumento(Connection conexao) throws SQLException {
        for (String tabela : List.of("empreendedor", "solicitacao_publica")) {
            Set<String> nomes = colunas(conexao, tabela);
            assertTrue(nomes.containsAll(Set.of("documento", "tipo_documento")), tabela + ": " + nomes);
            assertFalse(nomes.contains("cnpj"), tabela + " não tem mais a coluna cnpj");
        }
    }

    private static void conferirPersonas(Connection conexao) throws SQLException {
        Map<String, BigDecimal> esperado = new LinkedHashMap<>();
        esperado.put("P1", null);
        esperado.put("P2", new BigDecimal("30.00"));
        esperado.put("P3", new BigDecimal("20.00"));
        esperado.put("P4", new BigDecimal("10.00"));
        esperado.put("P5", new BigDecimal("10.00"));
        esperado.put("P6", null);
        Map<String, BigDecimal> lido = new LinkedHashMap<>();
        List<String> nomes = new ArrayList<>();
        try (Statement st = conexao.createStatement();
                ResultSet rs = st.executeQuery("SELECT codigo, nome, cota_mensal_horas FROM persona ORDER BY codigo")) {
            while (rs.next()) {
                lido.put(rs.getString(1), rs.getBigDecimal(3));
                nomes.add(rs.getString(2));
            }
        }
        assertEquals(esperado, lido);
        assertTrue(nomes.contains("Governo, ICTs e 3º setor"), "acentos e símbolos preservados: " + nomes);
    }

    private static void conferirConfiguracao(Connection conexao) throws SQLException {
        assertEquals(List.of("1|4|08:00:00|20:00:00"), linhas(conexao,
                "SELECT id, horas_por_turno, abertura, fechamento FROM configuracao"));
    }

    private static void conferirPrecos(Connection conexao) throws SQLException {
        assertEquals(List.of("1|50|800.00", "1|75|1200.00", "1|100|1600.00", "2|50|1400.00", "2|75|2100.00",
                "2|100|2800.00", "3|50|1800.00", "3|75|2700.00", "3|100|3600.00"),
                linhas(conexao,
                        "SELECT quantidade_turnos, percentual, valor FROM tabela_preco WHERE ano = 2026 AND ativo"
                                + " ORDER BY quantidade_turnos, percentual"));
        assertEquals(List.of("9"), linhas(conexao, "SELECT COUNT(*) FROM tabela_preco"), "só os preços de 2026");
    }

    private static void conferirFeriados(Connection conexao) throws SQLException {
        assertEquals(List.of("2026-10-12|Nossa Senhora Aparecida", "2026-11-02|Finados",
                "2026-11-15|Proclamação da República", "2026-11-20|Consciência Negra", "2026-12-25|Natal"),
                linhas(conexao, "SELECT data, descricao FROM feriado WHERE ativo ORDER BY data"));
    }

    private static void conferirEspacos(Connection conexao) throws SQLException {
        assertEquals(List.of("Auditório|114|114 cadeiras", "Sala de reunião|16|16 cadeiras", "Coworking|35|35 cadeiras",
                "Sala de Reunião 1 (coworking)|6|6 cadeiras", "Sala de Reunião 2 (coworking)|6|6 cadeiras",
                "Sala Pré-incubação|34|34 cadeiras", "Sala Maker VIA|32|32 cadeiras e 6 sofás",
                "Sala VIA hub|15|15 cadeiras, 7 sofás e 1 arquibancada", "Sala Distrito de Inovação|42|42 cadeiras",
                "Sala diária de trabalho|18|14 cadeiras e 4 cadeiras brancas na mesa redonda"),
                linhas(conexao, "SELECT nome_espaco, capacidade, mobiliario FROM espaco WHERE ativo AND disponibilidade"
                        + " ORDER BY id_espaco"));
        assertEquals(List.of("10"), linhas(conexao, "SELECT COUNT(*) FROM espaco"));
    }

    /** Regras que o próprio banco garante: documento único mas opcional, capacidade e horário válidos. */
    private static void conferirRegrasDoBanco(Connection conexao) throws SQLException {
        conexao.setAutoCommit(false);
        try {
            String empreendedor = "INSERT INTO empreendedor (documento, tipo_documento, nome, email, ativo)"
                    + " VALUES (?, ?, ?, ?, TRUE)";
            inserir(conexao, empreendedor, null, "SEM", "Sem documento A", "a@exemplo.invalid");
            inserir(conexao, empreendedor, null, "SEM", "Sem documento B", "b@exemplo.invalid");
            inserir(conexao, empreendedor, "12345678000190", "CNPJ", "Empresa", "c@exemplo.invalid");
            assertThrows(SQLException.class, () -> inserir(conexao, empreendedor, "12345678000190", "CNPJ", "Outra",
                    "d@exemplo.invalid"), "documento repetido é recusado");
            conexao.rollback();
            assertThrows(SQLException.class, () -> inserir(conexao,
                    "INSERT INTO espaco (nome_espaco, capacidade, disponibilidade, ativo) VALUES (?, 0, TRUE, TRUE)",
                    "Sem lugar"), "capacidade zero é recusada");
            conexao.rollback();
        } finally {
            conexao.rollback();
            conexao.setAutoCommit(true);
        }
        assertEquals(List.of("0"), linhas(conexao, "SELECT COUNT(*) FROM empreendedor"));
    }

    private static void inserir(Connection conexao, String sql, Object... valores) throws SQLException {
        try (PreparedStatement st = conexao.prepareStatement(sql)) {
            for (int i = 0; i < valores.length; i++) {
                st.setObject(i + 1, valores[i]);
            }
            st.executeUpdate();
        }
    }

    private static Set<String> colunas(Connection conexao, String tabela) throws SQLException {
        Set<String> nomes = new TreeSet<>();
        DatabaseMetaData meta = conexao.getMetaData();
        for (String nome : List.of(tabela, tabela.toUpperCase(Locale.ROOT))) {
            try (ResultSet rs = meta.getColumns(conexao.getCatalog(), conexao.getSchema(), nome, "%")) {
                while (rs.next()) {
                    nomes.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                }
            }
        }
        return nomes;
    }

    private static List<String> linhas(Connection conexao, String sql) throws SQLException {
        List<String> resultado = new ArrayList<>();
        try (Statement st = conexao.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            int total = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                List<String> campos = new ArrayList<>();
                for (int i = 1; i <= total; i++) {
                    campos.add(String.valueOf(rs.getString(i)));
                }
                resultado.add(String.join("|", campos));
            }
        }
        return resultado;
    }
}

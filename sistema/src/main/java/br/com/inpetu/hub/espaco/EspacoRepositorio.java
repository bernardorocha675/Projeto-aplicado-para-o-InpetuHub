package br.com.inpetu.hub.espaco;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

/** Espaços em JDBC puro, sempre em ordem fixa (nome e número). */
public final class EspacoRepositorio {

    private static final String COLUNAS = "select id_espaco, nome_espaco, capacidade, mobiliario, disponibilidade, ativo"
            + " from espaco ";
    private static final String ORDEM = " order by nome_espaco, id_espaco";

    private final DataSource banco;

    public EspacoRepositorio(DataSource banco) {
        this.banco = banco;
    }

    public List<Espaco> todos() {
        return lista(COLUNAS + ORDEM);
    }

    /** Ativos e disponíveis para reservas. */
    public List<Espaco> reservaveis() {
        return lista(COLUNAS + "where ativo = true and disponibilidade = true" + ORDEM);
    }

    public Optional<Espaco> porId(long id) {
        try (Connection conexao = banco.getConnection();
                PreparedStatement sql = conexao.prepareStatement(COLUNAS
                        + "where id_espaco = ?")) {
            sql.setLong(1, id);
            try (ResultSet linha = sql.executeQuery()) {
                return linha.next() ? Optional.of(ler(linha)) : Optional.empty();
            }
        } catch (SQLException falha) {
            throw new IllegalStateException("Não foi possível ler o espaço.", falha);
        }
    }

    /** Grava um espaço novo e devolve o número dele. */
    long inserir(EspacoFormulario.Dados dados) {
        try (Connection conexao = banco.getConnection();
                PreparedStatement sql = conexao.prepareStatement(
                        "insert into espaco(nome_espaco, capacidade, mobiliario, disponibilidade, ativo) values (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
            preencher(sql, dados);
            sql.executeUpdate();
            try (ResultSet chave = sql.getGeneratedKeys()) {
                chave.next();
                return chave.getLong(1);
            }
        } catch (SQLException falha) {
            throw new IllegalStateException("Não foi possível gravar o espaço.", falha);
        }
    }

    boolean atualizar(long id, EspacoFormulario.Dados dados) {
        try (Connection conexao = banco.getConnection();
                PreparedStatement sql = conexao.prepareStatement(
                        "update espaco set nome_espaco = ?, capacidade = ?, mobiliario = ?, disponibilidade = ?, ativo = ?"
                                + " where id_espaco = ?")) {
            preencher(sql, dados);
            sql.setLong(6, id);
            return sql.executeUpdate() == 1;
        } catch (SQLException falha) {
            throw new IllegalStateException("Não foi possível gravar o espaço.", falha);
        }
    }

    /** Apaga o espaço. Com reservas ligadas a ele, o banco recusa (a tela pede para desativar). */
    boolean excluir(long id) {
        try (Connection conexao = banco.getConnection();
                PreparedStatement sql = conexao.prepareStatement(
                        "delete from espaco where id_espaco = ?")) {
            sql.setLong(1, id);
            return sql.executeUpdate() == 1;
        } catch (SQLException falha) {
            throw new IllegalStateException("Não foi possível excluir o espaço.", falha);
        }
    }

    private List<Espaco> lista(String consulta) {
        List<Espaco> saida = new ArrayList<>();
        try (Connection conexao = banco.getConnection();
                PreparedStatement sql = conexao.prepareStatement(consulta);
                ResultSet linha = sql.executeQuery()) {
            while (linha.next()) {
                saida.add(ler(linha));
            }
            return saida;
        } catch (SQLException falha) {
            throw new IllegalStateException("Não foi possível ler os espaços.", falha);
        }
    }

    private static void preencher(PreparedStatement sql, EspacoFormulario.Dados dados) throws SQLException {
        sql.setString(1, dados.nome());
        sql.setInt(2, dados.capacidade());
        sql.setString(3, dados.mobiliario());
        sql.setBoolean(4, dados.disponivel());
        sql.setBoolean(5, dados.ativo());
    }

    private static Espaco ler(ResultSet linha) throws SQLException {
        return new Espaco(linha.getLong(1), linha.getString(2), linha.getInt(3), linha.getString(4), linha.getBoolean(
                5), linha.getBoolean(6));
    }
}

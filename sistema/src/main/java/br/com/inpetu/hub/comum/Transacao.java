package br.com.inpetu.hub.comum;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

/**
 * Uma gravação de reservas e pedidos de cada vez: a transação trava a linha da configuração (como o antigo fazia),
 * então duas aprovações ao mesmo tempo nunca ocupam o mesmo horário nem a mesma cota. Tudo ou nada: em qualquer erro,
 * nada fica gravado.
 */
public final class Transacao {

    /** O trabalho feito dentro da transação, com a conexão travada. */
    public interface Trabalho<T> {

        T executar(Connection conexao) throws SQLException;
    }

    private Transacao() {
    }

    public static <T> T executar(DataSource banco, Trabalho<T> trabalho) {
        try (Connection conexao = banco.getConnection()) {
            conexao.setAutoCommit(false);
            try {
                travar(conexao);
                T resultado = trabalho.executar(conexao);
                conexao.commit();
                return resultado;
            } catch (SQLException | RuntimeException | Error falha) {
                conexao.rollback();
                throw falha;
            } finally {
                conexao.setAutoCommit(true);
            }
        } catch (SQLException falha) {
            throw new IllegalStateException("Não foi possível gravar.", falha);
        }
    }

    private static void travar(Connection conexao) throws SQLException {
        try (PreparedStatement sql = conexao.prepareStatement("select id from configuracao where id = 1 for update");
                ResultSet linha = sql.executeQuery()) {
            if (!linha.next()) {
                throw new IllegalStateException("Configuração do Hub não encontrada.");
            }
        }
    }
}

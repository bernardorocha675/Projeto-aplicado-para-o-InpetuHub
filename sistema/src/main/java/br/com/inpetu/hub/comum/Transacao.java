package br.com.inpetu.hub.comum;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;

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

    // Uma vez por processo e antes de pegar a conexão: quem espera não ocupa o conjunto, e quem grava sempre consegue
    // outra conexão para ler. A trava da linha continua valendo entre processos (MySQL).
    private static final ReentrantLock UMA_POR_VEZ = new ReentrantLock(true);

    private Transacao() {
    }

    public static <T> T executar(DataSource banco, Trabalho<T> trabalho) {
        UMA_POR_VEZ.lock();
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
        } finally {
            UMA_POR_VEZ.unlock();
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

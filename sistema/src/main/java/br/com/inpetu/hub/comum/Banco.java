package br.com.inpetu.hub.comum;

import com.mysql.cj.jdbc.MysqlDataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcConnectionPool;

/**
 * Onde o sistema guarda os dados: H2 em arquivo no computador (modo local) ou MySQL. Os dois rodam as mesmas
 * migrations; o H2 fica no modo MySQL para aceitar o mesmo SQL.
 */
public final class Banco {

    private static final String OPCOES_H2 = ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE";

    private Banco() {
    }

    /**
     * Banco H2 em arquivo. {@code arquivo} é o caminho sem extensão (o H2 cria {@code arquivo.mv.db}); a pasta é criada
     * se faltar. O caminho pode ter espaços e acentos, mas não ponto e vírgula, que o H2 lê como opção.
     */
    public static DataSource h2Arquivo(Path arquivo) {
        Path absoluto = arquivo.toAbsolutePath().normalize();
        if (absoluto.toString().contains(";")) {
            throw new IllegalArgumentException("A pasta dos dados não pode ter ponto e vírgula no nome.");
        }
        try {
            Files.createDirectories(absoluto.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível criar a pasta dos dados.", e);
        }
        return JdbcConnectionPool.create("jdbc:h2:file:" + absoluto + OPCOES_H2, "", "");
    }

    /** Banco H2 só na memória, apagado quando o processo termina; usado nos testes. */
    public static DataSource h2Memoria(String nome) {
        return JdbcConnectionPool.create("jdbc:h2:mem:" + nome + OPCOES_H2, "", "");
    }

    /** MySQL: uma conexão nova a cada pedido, sem pool (plano da reescrita). */
    public static DataSource mysql(String url, String usuario, String senha) {
        MysqlDataSource fonte = new MysqlDataSource();
        fonte.setUrl(url);
        fonte.setUser(usuario);
        fonte.setPassword(senha);
        return fonte;
    }
}

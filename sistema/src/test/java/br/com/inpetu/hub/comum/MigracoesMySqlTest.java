package br.com.inpetu.hub.comum;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

/**
 * As mesmas migrations num MySQL de verdade. Roda quando INPETU_TESTE_MYSQL_URL existe (a CI sobe um serviço MySQL);
 * com INPETU_EXIGIR_MYSQL=1, a falta do MySQL é falha, para a CI nunca pular este teste sem avisar.
 */
class MigracoesMySqlTest {

    @Test
    void modeloEDadosIniciaisNoMySql() throws Exception {
        String url = System.getenv("INPETU_TESTE_MYSQL_URL");
        boolean exigido = "1".equals(System.getenv("INPETU_EXIGIR_MYSQL"));
        boolean temUrl = url != null && !url.isBlank();
        if (exigido) {
            assertTrue(temUrl, "INPETU_EXIGIR_MYSQL=1, mas INPETU_TESTE_MYSQL_URL não foi definida");
        }
        assumeTrue(temUrl, "sem INPETU_TESTE_MYSQL_URL: teste do MySQL pulado nesta máquina");
        DataSource banco = Banco.mysql(url, System.getenv("INPETU_TESTE_MYSQL_USUARIO"),
                System.getenv("INPETU_TESTE_MYSQL_SENHA"));
        limpar(banco);
        try {
            ConferenciaDoModelo.conferirTudo(banco);
        } finally {
            limpar(banco);
        }
    }

    /** O banco de teste começa e termina vazio. */
    private static void limpar(DataSource banco) throws Exception {
        try (Connection conexao = banco.getConnection(); Statement st = conexao.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS = 0");
            for (String tabela : ConferenciaDoModelo.TABELAS) {
                st.execute("DROP TABLE IF EXISTS " + tabela);
            }
            st.execute("DROP TABLE IF EXISTS flyway_schema_history");
            st.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }
}

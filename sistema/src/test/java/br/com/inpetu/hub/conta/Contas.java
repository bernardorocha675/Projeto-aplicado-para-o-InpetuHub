package br.com.inpetu.hub.conta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

/** Apoio dos testes: cria contas de teste e lê a auditoria direto no banco. */
final class Contas {

    static final String SENHA = "Senha-sintetica-de-teste-2026!";

    record Criada(long id, String login) {
    }

    private Contas() {
    }

    static Criada criar(DataSource banco, Perfil perfil) throws Exception {
        String login = "teste-" + UUID.randomUUID();
        try (Connection c = banco.getConnection();
                PreparedStatement sql = c.prepareStatement(
                        "insert into colaborador(nome, email, login, senha, perfil, ativo, security_version)"
                                + " values ('Pessoa de teste', '', ?, ?, ?, true, 1)",
                        Statement.RETURN_GENERATED_KEYS)) {
            sql.setString(1, login);
            sql.setString(2, Senhas.hash(SENHA));
            sql.setString(3, perfil.name());
            sql.executeUpdate();
            try (ResultSet chave = sql.getGeneratedKeys()) {
                chave.next();
                return new Criada(chave.getLong(1), login);
            }
        }
    }

    static void executar(DataSource banco, String comando, Object... valores) throws Exception {
        try (Connection c = banco.getConnection(); PreparedStatement sql = c.prepareStatement(comando)) {
            for (int i = 0; i < valores.length; i++) {
                sql.setObject(i + 1, valores[i]);
            }
            sql.executeUpdate();
        }
    }

    static String texto(DataSource banco, String consulta, Object... valores) throws Exception {
        try (Connection c = banco.getConnection(); PreparedStatement sql = c.prepareStatement(consulta)) {
            for (int i = 0; i < valores.length; i++) {
                sql.setObject(i + 1, valores[i]);
            }
            try (ResultSet linha = sql.executeQuery()) {
                return linha.next() ? linha.getString(1) : null;
            }
        }
    }

    /** As linhas da auditoria, da mais antiga para a mais nova, como "EVENTO RESULTADO autor". */
    static List<String> auditoria(DataSource banco) throws Exception {
        List<String> linhas = new ArrayList<>();
        try (Connection c = banco.getConnection();
                PreparedStatement sql = c.prepareStatement(
                        "select evento, resultado, autor_id, alvo from auditoria order by id");
                ResultSet linha = sql
                        .executeQuery()) {
            while (linha.next()) {
                linhas.add(linha.getString(1) + " " + linha.getString(2) + " " + linha.getObject(3) + " " + linha
                        .getString(4));
            }
        }
        return linhas;
    }
}

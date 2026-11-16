package br.com.inpetu.hub.conta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import javax.sql.DataSource;

/** Leitura e gravação das contas (tabela {@code colaborador}) em JDBC puro. */
final class ContaRepositorio {

    private static final String COLUNAS = "select id_colaborador, nome, login, senha, perfil, ativo, security_version"
            + " from colaborador ";

    private final DataSource banco;

    ContaRepositorio(DataSource banco) {
        this.banco = banco;
    }

    /** Pelo usuário já normalizado (sem diferenciar maiúsculas). */
    Optional<Conta> porLogin(String login) {
        return uma(COLUNAS + "where lower(login) = ?", login);
    }

    Optional<Conta> porId(long id) {
        return uma(COLUNAS + "where id_colaborador = ?", id);
    }

    /**
     * Grava a nova senha e sobe a versão, o que derruba as sessões abertas. Só grava se a conta ainda estiver na versão
     * lida: devolve false se outra mudança chegou antes.
     */
    boolean trocarSenha(Conta conta, String novoHash) {
        return executar("update colaborador set senha = ?, security_version = security_version + 1"
                + " where id_colaborador = ? and security_version = ?", novoHash, conta.id(), conta.versao()) == 1;
    }

    void inserir(String nome, String login, String hash, Perfil perfil) {
        executar("insert into colaborador(nome, email, login, senha, perfil, ativo, security_version)"
                + " values (?, '', ?, ?, ?, true, 1)", nome, login, hash, perfil.name());
    }

    /** Volta a conta ao padrão (nome, senha, perfil, ativa) e sobe a versão. */
    void restaurar(long id, String nome, String hash, Perfil perfil) {
        executar("update colaborador set nome = ?, senha = ?, perfil = ?, ativo = true,"
                + " security_version = security_version + 1 where id_colaborador = ?", nome, hash, perfil.name(), id);
    }

    private Optional<Conta> uma(String consulta, Object valor) {
        try (Connection conexao = banco.getConnection(); PreparedStatement sql = conexao.prepareStatement(consulta)) {
            sql.setObject(1, valor);
            try (ResultSet linha = sql.executeQuery()) {
                if (!linha.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Conta(linha.getLong(1), linha.getString(2), linha.getString(3), linha
                        .getString(4), perfil(linha.getString(5)), linha.getBoolean(6), linha.getLong(7)));
            }
        } catch (SQLException falha) {
            throw new IllegalStateException("Não foi possível ler a conta.", falha);
        }
    }

    private int executar(String comando, Object... valores) {
        try (Connection conexao = banco.getConnection(); PreparedStatement sql = conexao.prepareStatement(comando)) {
            for (int i = 0; i < valores.length; i++) {
                sql.setObject(i + 1, valores[i]);
            }
            return sql.executeUpdate();
        } catch (SQLException falha) {
            throw new IllegalStateException("Não foi possível gravar a conta.", falha);
        }
    }

    /** Perfil desconhecido no banco vira COLABORADOR: nunca dá mais acesso do que o mínimo. */
    private static Perfil perfil(String valor) {
        return "ADMIN".equals(valor) ? Perfil.ADMIN : Perfil.COLABORADOR;
    }
}

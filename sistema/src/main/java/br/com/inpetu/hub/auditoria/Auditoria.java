package br.com.inpetu.hub.auditoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Clock;
import java.time.LocalDateTime;

import javax.sql.DataSource;

import br.com.inpetu.hub.comum.log.Eventos;

/**
 * Grava a auditoria no banco (a tela fica para a Administração). Nunca recebe senha, formulário nem o usuário digitado:
 * os detalhes são textos montados pelo código. Cada registro se grava sozinho, então uma tentativa negada fica
 * registrada mesmo que outra operação falhe.
 */
public final class Auditoria {

    /** Alvo dos eventos de login, sessão e acesso. */
    public static final String AUTENTICACAO = "autenticacao";

    private final DataSource banco;
    private final Clock relogio;

    public Auditoria(DataSource banco, Clock relogio) {
        this.banco = banco;
        this.relogio = relogio;
    }

    public void registrar(Long autor, String evento, String alvo, Long alvoId, String resultado, String detalhes) {
        try (Connection conexao = banco.getConnection();
                PreparedStatement sql = conexao.prepareStatement(
                        "insert into auditoria(instante, autor_id, evento, alvo, alvo_id, resultado, detalhes)"
                                + " values (?, ?, ?, ?, ?, ?, ?)")) {
            sql.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now(relogio)));
            definir(sql, 2, autor);
            sql.setString(3, evento);
            sql.setString(4, alvo);
            definir(sql, 5, alvoId);
            sql.setString(6, resultado);
            sql.setString(7, detalhes);
            sql.executeUpdate();
        } catch (SQLException falha) {
            throw new IllegalStateException("Não foi possível gravar a auditoria.", falha);
        }
        Eventos.auditoria(evento, alvo, resultado);
    }

    /** Login, sessão e acesso: o alvo é a própria conta. */
    public void seguranca(Long conta, String evento, String resultado) {
        registrar(conta, evento, AUTENTICACAO, conta, resultado, null);
    }

    private static void definir(PreparedStatement sql, int posicao, Long valor) throws SQLException {
        if (valor == null) {
            sql.setNull(posicao, Types.BIGINT);
        } else {
            sql.setLong(posicao, valor);
        }
    }
}

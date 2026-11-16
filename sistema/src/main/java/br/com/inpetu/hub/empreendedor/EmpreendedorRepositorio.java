package br.com.inpetu.hub.empreendedor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import br.com.inpetu.hub.comum.RegraException;
import br.com.inpetu.hub.solicitacao.PedidoConferido;

/**
 * O vínculo do pedido com o cadastro do empreendedor (o cadastro completo vem depois da v1). Com documento, acha pelo
 * número; sem documento, pelo email, sem diferenciar maiúsculas. Achou: usa o cadastro como está, sem sobrescrever.
 */
public final class EmpreendedorRepositorio {

    /** Devolve o número do cadastro do solicitante, criando-o se não existir. Recusa cadastro desativado. */
    public long vincular(Connection conexao, PedidoConferido p) throws SQLException {
        String consulta = p.documento() != null
                ? "select id_empreendedor, ativo from empreendedor where documento = ?"
                : "select id_empreendedor, ativo from empreendedor where lower(email) = lower(?) order by id_empreendedor";
        try (PreparedStatement sql = conexao.prepareStatement(consulta)) {
            sql.setString(1, p.documento() != null ? p.documento() : p.email());
            try (ResultSet linha = sql.executeQuery()) {
                if (linha.next()) {
                    if (!linha.getBoolean(2)) {
                        throw new RegraException("Cadastro inativo. Entre em contato com a equipe.");
                    }
                    return linha.getLong(1);
                }
            }
        }
        try (PreparedStatement sql = conexao.prepareStatement("insert into empreendedor(documento, tipo_documento,"
                + " nome, email, telefone, cidade, empresa, segmento, cargo, ativo) values (?, ?, ?, ?, ?, ?, ?, ?, ?,"
                + " true)", Statement.RETURN_GENERATED_KEYS)) {
            String[] valores = {p.documento(), p.tipoDocumento(), p.nome(), p.email(), p.telefone(), p.cidade(), p
                    .empresa(), p.segmento(), p.cargo()};
            for (int i = 0; i < valores.length; i++) {
                if (valores[i] == null) {
                    sql.setNull(i + 1, Types.VARCHAR);
                } else {
                    sql.setString(i + 1, valores[i]);
                }
            }
            sql.executeUpdate();
            try (ResultSet chave = sql.getGeneratedKeys()) {
                chave.next();
                return chave.getLong(1);
            }
        }
    }
}

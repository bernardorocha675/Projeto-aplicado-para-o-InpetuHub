package br.com.inpetu.hub.conta;

import java.io.Serial;
import java.io.Serializable;

/**
 * A conta guardada na sessão depois do login. A cada pedido é comparada com o banco: se a conta foi desativada ou mudou
 * de senha, perfil ou usuário (a versão sobe), a sessão cai.
 */
public record ContaSessao(long id, String nome, String login, Perfil perfil, long versao, long iniciadaEm)
        implements
            Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    static ContaSessao de(Conta conta, long agora) {
        return new ContaSessao(conta.id(), conta.nome(), conta.login(), conta.perfil(), conta.versao(), agora);
    }

    boolean confere(Conta atual) {
        return atual.ativa() && atual.versao() == versao && atual.perfil() == perfil && atual.login().equals(login);
    }

    @Override
    public String toString() {
        return "ContaSessao[" + id + "]";
    }
}

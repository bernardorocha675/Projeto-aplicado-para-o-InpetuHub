package br.com.inpetu.hub.conta;

import java.util.List;
import java.util.Optional;

import br.com.inpetu.hub.auditoria.Auditoria;
import br.com.inpetu.hub.comum.Ambiente;

/**
 * As 5 contas PÚBLICAS da demonstração local (as mesmas do antigo, em docs/ACESSOS.md). Nunca reutilizar estas senhas
 * em outro serviço. No modo local, voltam ao padrão sempre que o sistema inicia.
 */
public final class ContasDemonstracao {

    public record Demonstracao(String nome, String login, String senha, Perfil perfil) {
    }

    public static final List<Demonstracao> TODAS = List.of(
            new Demonstracao("Milena", "milena", "milena040", Perfil.COLABORADOR),
            new Demonstracao("Bernardo", "bernardo", "bernardo040", Perfil.ADMIN),
            new Demonstracao("João F", "joaof", "joaof040", Perfil.COLABORADOR),
            new Demonstracao("João E", "joaoe", "joaoe040", Perfil.COLABORADOR),
            new Demonstracao("Guilherme", "guilherme", "guilherme040", Perfil.COLABORADOR));

    private ContasDemonstracao() {
    }

    /**
     * Cria as que faltam e volta ao padrão as que mudaram (senha, perfil, nome ou desativada), subindo a versão para
     * derrubar sessões antigas. Devolve quantas mudaram. Só no modo local.
     */
    public static int restaurar(Ambiente ambiente) {
        if (!ambiente.modoLocal()) {
            throw new IllegalStateException("As contas de demonstração só existem no modo local.");
        }
        ContaRepositorio contas = new ContaRepositorio(ambiente.banco());
        int mudaram = 0;
        for (Demonstracao padrao : TODAS) {
            Optional<Conta> atual = contas.porLogin(padrao.login());
            if (atual.isEmpty()) {
                contas.inserir(padrao.nome(), padrao.login(), Senhas.hash(padrao.senha()), padrao.perfil());
                mudaram++;
            } else if (!igual(atual.get(), padrao)) {
                contas.restaurar(atual.get().id(), padrao.nome(), Senhas.hash(padrao.senha()), padrao.perfil());
                mudaram++;
            }
        }
        if (mudaram > 0) {
            new Auditoria(ambiente.banco(), ambiente.relogio()).registrar(null, "CONTAS_ATUALIZADAS", "contas", null,
                    "OK", "Contas de demonstração restauradas: " + mudaram + ".");
        }
        return mudaram;
    }

    private static boolean igual(Conta conta, Demonstracao padrao) {
        return conta.ativa() && conta.perfil() == padrao.perfil() && padrao.nome().equals(conta.nome()) && padrao
                .login().equals(conta.login()) && Senhas.confere(padrao.senha(), conta.hash());
    }
}

package br.com.inpetu.hub.comum;

import java.sql.SQLIntegrityConstraintViolationException;

/** Decide a situação HTTP e o texto da tela para cada erro, sem nunca mostrar detalhe técnico. */
final class Erros {

    record Tela(int status, String titulo, String mensagem) {
    }

    private static final String TENTE_DE_NOVO = "Tente novamente. Se persistir, consulte o log local do servidor.";

    private Erros() {
    }

    /** Erro que veio de uma exceção: regra recusada (400), conflito no banco (409) ou falha inesperada (500). */
    static Tela daExcecao(Throwable falha) {
        for (Throwable t = falha; t != null; t = t.getCause()) {
            if (t instanceof RegraException regra) {
                return new Tela(400, "Confira os dados", regra.getMessage());
            }
            if (t instanceof SQLIntegrityConstraintViolationException) {
                return new Tela(409, "Não foi possível concluir",
                        "Registro duplicado ou vinculado a um histórico. Confira os dados ou desative o cadastro.");
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return daSituacao(500);
    }

    /** Erro que veio só da situação HTTP (página inexistente, método errado, acesso negado...). */
    static Tela daSituacao(int status) {
        return switch (status) {
            case 400 -> new Tela(400, "Confira os dados", "Confira os dados e tente novamente.");
            case 403 -> new Tela(403, "Acesso restrito",
                    "Esta ação exige permissão ou uma sessão válida. Recarregue a página e tente de novo.");
            case 404 -> new Tela(404, "Página não encontrada", "O endereço pode ter mudado ou não existir mais.");
            case 405 -> new Tela(405, "Ação não permitida", "Esta ação não está disponível neste endereço.");
            default -> status >= 500
                    ? new Tela(status, "Não foi possível concluir", TENTE_DE_NOVO)
                    : new Tela(status, "Não foi possível concluir", "Confira os dados e tente novamente.");
        };
    }
}

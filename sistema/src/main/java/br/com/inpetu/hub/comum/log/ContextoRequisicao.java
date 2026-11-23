package br.com.inpetu.hub.comum.log;

/**
 * O identificador da requisição em andamento nesta thread, para o log. O JUL não tem MDC: o filtro da requisição define
 * e limpa o valor.
 */
public final class ContextoRequisicao {

    private static final ThreadLocal<String> ID = new ThreadLocal<>();

    private ContextoRequisicao() {
    }

    public static void definir(String id) {
        ID.set(id);
    }

    public static String atual() {
        return ID.get();
    }

    public static void limpar() {
        ID.remove();
    }
}

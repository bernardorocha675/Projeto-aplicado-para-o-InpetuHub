package br.com.inpetu.hub.comum.log;

import java.util.Set;

/** O log guarda o tipo da tela ou da ação, nunca IDs, parâmetros ou caminhos escritos livremente. */
public final class RotasSeguras {

    // Cada entrega acrescenta aqui as rotas que criar.
    private static final Set<String> CONHECIDAS = Set.of("/", "/saude", "/tela", "/login", "/sair", "/admin",
            "/conta/senha",
            "/agenda", "/disponibilidade", "/admin/cadastros/espacos", "/admin/cadastros/espacos/editar",
            "/admin/cadastros/espacos/salvar", "/admin/cadastros/espacos/excluir", "/reserva", "/reservas/resumo",
            "/reservas/solicitar", "/reservas/recebida", "/admin/reservas", "/admin/solicitacoes",
            "/admin/solicitacoes/aceitar", "/admin/solicitacoes/recusar");
    private static final Set<String> ARQUIVOS = Set.of("/css/", "/js/", "/img/", "/fontes/");

    private RotasSeguras() {
    }

    public static String canonica(String caminho) {
        if (caminho == null || caminho.length() > 200) {
            return "OTHER";
        }
        if (CONHECIDAS.contains(caminho)) {
            return caminho;
        }
        // Números de reserva nunca vão para o log: só o tipo da tela ou da ação.
        if (caminho.matches("/admin/reservas/[0-9]{1,18}")) {
            return "/admin/reservas/{id}";
        }
        if (caminho.matches("/admin/reservas/[0-9]{1,18}/(aprovar|recusar|cancelar|alterar|pagamento|concluir)")) {
            return "/admin/reservas/{id}/" + caminho.substring(caminho.lastIndexOf('/') + 1);
        }
        for (String pasta : ARQUIVOS) {
            if (caminho.startsWith(pasta)) {
                return "ASSET";
            }
        }
        return caminho.equals("/favicon.ico") ? "ASSET" : "OTHER";
    }
}

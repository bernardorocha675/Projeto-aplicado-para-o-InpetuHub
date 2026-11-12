package br.com.inpetu.hub.comum.log;

import java.util.Set;

/** O log guarda o tipo da tela ou da ação, nunca IDs, parâmetros ou caminhos escritos livremente. */
public final class RotasSeguras {

    // Cada entrega acrescenta aqui as rotas que criar.
    private static final Set<String> CONHECIDAS = Set.of("/saude");
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
        for (String pasta : ARQUIVOS) {
            if (caminho.startsWith(pasta)) {
                return "ASSET";
            }
        }
        return caminho.equals("/favicon.ico") ? "ASSET" : "OTHER";
    }
}

package br.com.inpetu.hub.solicitacao;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.com.inpetu.hub.comum.Http;

/** Apoio dos testes: abre o formulário (que traz o token) e envia o pedido. */
final class Pedidos {

    private Pedidos() {
    }

    static HttpResponse<String> resumo(Http http, Map<String, String> campos) throws Exception {
        String token = Http.csrf(http.get("/reserva").body());
        List<String> pares = new ArrayList<>(List.of("_csrf", token));
        campos.forEach((nome, valor) -> {
            pares.add(nome);
            pares.add(valor);
        });
        return http.post("/reservas/resumo", Http.formulario(pares.toArray(String[]::new)));
    }

    static HttpResponse<String> confirmar(Http http, String resumo) throws Exception {
        return http.post("/reservas/solicitar", Http.formulario("_csrf", Http.csrf(resumo)));
    }

    static Map<String, String> campos(long espacoId) {
        Map<String, String> c = PedidoFormularioTest.campos();
        c.put("espacoId", String.valueOf(espacoId));
        return c;
    }
}

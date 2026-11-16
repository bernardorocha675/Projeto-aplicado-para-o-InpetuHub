package br.com.inpetu.hub.conta;

import java.net.http.HttpResponse;

import br.com.inpetu.hub.comum.Http;

/** Abre a tela de login (que traz o token CSRF) e envia o usuário e a senha. */
public final class Entrar {

    private Entrar() {
    }

    public static HttpResponse<String> com(Http http, String usuario, String senha) throws Exception {
        String token = Http.csrf(http.get("/login").body());
        return http.post("/login", Http.formulario("_csrf", token, "usuario", usuario, "senha", senha));
    }

    /** Envia um formulário de uma tela já aberta, com o token dela. */
    public static HttpResponse<String> enviar(Http http, String tela, String destino, String... campos)
            throws Exception {
        String token = Http.csrf(http.get(tela).body());
        String[] pares = new String[campos.length + 2];
        pares[0] = "_csrf";
        pares[1] = token;
        System.arraycopy(campos, 0, pares, 2, campos.length);
        return http.post(destino, Http.formulario(pares));
    }

    public static String destino(HttpResponse<String> resposta) {
        return resposta.headers().firstValue("Location").orElse("");
    }
}

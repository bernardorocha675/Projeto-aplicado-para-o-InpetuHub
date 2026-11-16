package br.com.inpetu.hub.conta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.AmbienteDeTeste;
import br.com.inpetu.hub.comum.Http;
import br.com.inpetu.hub.comum.Servidor;

/** Porta do passwordChangeAcceptsBasicEightCharacterPassword e do passwordPageOffersWayBackWithoutChanging. */
@Timeout(120)
class TrocaDeSenhaTest {

    private static Ambiente ambiente;
    private static Servidor servidor;

    @BeforeAll
    static void subir() throws Exception {
        ambiente = AmbienteDeTeste.novo(true);
        servidor = Servidor.iniciar(0, ambiente);
    }

    @AfterAll
    static void descer() {
        servidor.close();
    }

    private static Http entrar(Contas.Criada conta) throws Exception {
        Http http = new Http(servidor);
        assertEquals("/admin", Entrar.destino(Entrar.com(http, conta.login(), Contas.SENHA)));
        return http;
    }

    @Test
    void telaOfereceVoltarSemAlterarEAvisaDoModoLocal() throws Exception {
        Http http = entrar(Contas.criar(ambiente.banco(), Perfil.COLABORADOR));
        String html = http.get("/conta/senha").body();
        assertTrue(html.contains("href=\"/admin\">Voltar sem alterar</a>"), html);
        assertTrue(html.contains("autocomplete=\"current-password\""));
        assertTrue(html.contains("autocomplete=\"new-password\""));
        assertTrue(html.contains("minlength=\"8\"") && html.contains("maxlength=\"64\""));
        assertTrue(html.contains("as senhas das contas de demonstração voltam ao padrão"), html);
        assertTrue(http.get("/admin").body().contains("href=\"/conta/senha\""), "o início leva à troca de senha");
    }

    @Test
    void trocaComSenhaSimplesDe8CaracteresEncerraASessao() throws Exception {
        Contas.Criada conta = Contas.criar(ambiente.banco(), Perfil.COLABORADOR);
        Http http = entrar(conta);
        Http outraAba = entrar(conta);
        HttpResponse<String> resposta = Entrar.enviar(http, "/conta/senha", "/conta/senha", "atual", Contas.SENHA,
                "nova", "Nova1234");
        assertEquals(303, resposta.statusCode(), resposta.body());
        assertEquals("/login?alterada", Entrar.destino(resposta));
        assertTrue(http.get("/login?alterada").body().contains("Senha alterada. Entre com a nova senha."));
        assertEquals(303, http.get("/admin").statusCode(), "a sessão desta aba terminou");
        assertEquals("/login?expirou", Entrar.destino(outraAba.get("/admin")), "e a das outras também");
        assertEquals("/login?erro", Entrar.destino(Entrar.com(new Http(servidor), conta.login(), Contas.SENHA)));
        assertEquals("/admin", Entrar.destino(Entrar.com(new Http(servidor), conta.login(), "Nova1234")));
        assertTrue(Contas.auditoria(ambiente.banco()).contains("SENHA_ALTERADA OK " + conta.id() + " colaboradores"));
    }

    @Test
    void senhaAtualErradaOuNovaForaDaRegraMostramOErroSemAlterar() throws Exception {
        Contas.Criada conta = Contas.criar(ambiente.banco(), Perfil.COLABORADOR);
        Http http = entrar(conta);
        String[][] casos = {{"errada-123", "Nova1234", "A senha atual não confere."},
                {Contas.SENHA, "Curta12", "Use uma nova senha de 8 a 64 caracteres (até 72 bytes)."},
                {Contas.SENHA, "x".repeat(65), "Use uma nova senha de 8 a 64 caracteres (até 72 bytes)."},
                {Contas.SENHA, "ç".repeat(37), "Use uma nova senha de 8 a 64 caracteres (até 72 bytes)."},
                {Contas.SENHA, Contas.SENHA, "A nova senha precisa ser diferente da atual."}};
        for (String[] caso : casos) {
            HttpResponse<String> resposta = Entrar.enviar(http, "/conta/senha", "/conta/senha", "atual", caso[0],
                    "nova", caso[1]);
            assertEquals(400, resposta.statusCode(), caso[2]);
            assertTrue(resposta.body().contains(caso[2]), resposta.body());
            assertTrue(resposta.body().contains("role=\"alert\""));
            assertTrue(resposta.body().contains("name=\"_csrf\""), "a tela volta pronta para tentar de novo");
        }
        assertEquals(200, http.get("/admin").statusCode(), "a sessão continua");
        assertEquals("/admin", Entrar.destino(Entrar.com(new Http(servidor), conta.login(), Contas.SENHA)));
    }

    @Test
    void trocaSemTokenEBarrada() throws Exception {
        Http http = entrar(Contas.criar(ambiente.banco(), Perfil.COLABORADOR));
        assertEquals(403, http.post("/conta/senha", Http.formulario("atual", Contas.SENHA, "nova", "Nova1234"))
                .statusCode());
    }
}

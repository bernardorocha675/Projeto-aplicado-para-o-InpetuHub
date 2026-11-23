package br.com.inpetu.hub.conta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.AmbienteDeTeste;
import br.com.inpetu.hub.comum.Http;
import br.com.inpetu.hub.comum.Servidor;

/**
 * Fora do modo local (sem contas de demonstração e com limite de login): revogação das sessões, prazo absoluto e
 * limites. Porta do previousSessionsAreRevoked e do productionLoginStillLimitsAttempts.
 */
@Timeout(120)
class SessaoTest {

    private static final AmbienteDeTeste.Relogio RELOGIO = new AmbienteDeTeste.Relogio();
    private static Ambiente ambiente;
    private static Servidor servidor;

    @BeforeAll
    static void subir() throws Exception {
        ambiente = AmbienteDeTeste.novo(false, RELOGIO);
        servidor = Servidor.iniciar(0, ambiente, RotasDeTesteConta.todas());
    }

    @AfterAll
    static void descer() {
        servidor.close();
    }

    @Test
    void foraDoModoLocalNaoHaContasDeDemonstracao() throws Exception {
        assertEquals("0", Contas.texto(ambiente.banco(), "select count(*) from colaborador where login = 'bernardo'"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"desativada", "senha", "perfil", "login"})
    void sessoesAbertasCaemQuandoAContaMuda(String mudanca) throws Exception {
        Contas.Criada conta = Contas.criar(ambiente.banco(), Perfil.COLABORADOR);
        Http primeira = new Http(servidor);
        Http segunda = new Http(servidor);
        Entrar.com(primeira, conta.login(), Contas.SENHA);
        Entrar.com(segunda, conta.login(), Contas.SENHA);
        assertEquals(200, primeira.get("/admin").statusCode());
        String coluna = switch (mudanca) {
            case "desativada" -> "ativo = false";
            case "senha" -> "senha = '" + Senhas.hash("Outra-senha-de-teste") + "'";
            case "perfil" -> "perfil = 'ADMIN'";
            default -> "login = 'outro-" + UUID.randomUUID() + "'";
        };
        Contas.executar(ambiente.banco(), "update colaborador set " + coluna
                + ", security_version = security_version + 1 where id_colaborador = ?", conta.id());

        HttpResponse<String> depois = primeira.get("/admin");
        assertEquals(303, depois.statusCode());
        assertEquals("/login?expirou", Entrar.destino(depois));
        assertEquals("/login?expirou", Entrar.destino(segunda.get("/conta/senha")));
        assertTrue(primeira.get("/login?expirou").body().contains("Sua sessão terminou."));
        assertEquals("/login", Entrar.destino(primeira.get("/admin")), "a sessão foi descartada");
        assertTrue(Contas.auditoria(ambiente.banco()).contains("SESSAO_REVOGADA NEGADO " + conta.id()
                + " autenticacao"));
    }

    @Test
    void sessaoTemPrazoAbsolutoDeDuasHoras() throws Exception {
        Contas.Criada conta = Contas.criar(ambiente.banco(), Perfil.COLABORADOR);
        Http http = new Http(servidor);
        Entrar.com(http, conta.login(), Contas.SENHA);
        RELOGIO.adiantar(Duration.ofMinutes(119));
        assertEquals(200, http.get("/admin").statusCode());
        RELOGIO.adiantar(Duration.ofMinutes(1).plusMillis(1));
        assertEquals("/login?expirou", Entrar.destino(http.get("/admin")));
    }

    @Test
    void foraDoModoLocalOLoginTemLimitePorConta() throws Exception {
        Contas.Criada conta = Contas.criar(ambiente.banco(), Perfil.ADMIN);
        for (int i = 0; i < 10; i++) {
            assertEquals("/login?erro", Entrar.destino(Entrar.com(new Http(servidor), conta.login(), "errada")));
        }
        HttpResponse<String> bloqueado = Entrar.com(new Http(servidor), conta.login().toUpperCase(), Contas.SENHA);
        assertEquals(429, bloqueado.statusCode(), "nem com a senha certa, até a janela passar");
        assertEquals("900", bloqueado.headers().firstValue("Retry-After").orElse(""));
        assertTrue(bloqueado.body().contains("Muitas tentativas"), bloqueado.body());
        RELOGIO.adiantar(Duration.ofMinutes(15).plusSeconds(1));
        assertEquals("/admin", Entrar.destino(Entrar.com(new Http(servidor), conta.login(), Contas.SENHA)),
                "o limite não trava a conta para sempre");
    }

    @Test
    void trocaDeSenhaTemLimiteDeTentativas() throws Exception {
        Contas.Criada conta = Contas.criar(ambiente.banco(), Perfil.COLABORADOR);
        Http http = new Http(servidor);
        Entrar.com(http, conta.login(), Contas.SENHA);
        String token = Http.csrf(http.get("/conta/senha").body());
        int ultima = 0;
        for (int i = 0; i < 13; i++) {
            ultima = http.post("/conta/senha", Http.formulario("_csrf", token, "atual", "errada", "nova",
                    "Nova-senha-1")).statusCode();
            if (i < 12) {
                assertEquals(400, ultima, "tentativa " + (i + 1));
            }
        }
        assertEquals(429, ultima);
        RELOGIO.adiantar(Duration.ofMinutes(5).plusSeconds(1));
    }
}

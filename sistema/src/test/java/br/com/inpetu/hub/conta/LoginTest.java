package br.com.inpetu.hub.conta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.AmbienteDeTeste;
import br.com.inpetu.hub.comum.Http;
import br.com.inpetu.hub.comum.Servidor;
import br.com.inpetu.hub.comum.log.CapturaDeLog;

/**
 * Login no modo local, com as 5 contas de demonstração. Porta do PublicDemoLoginTest, do
 * collaboratorCannotReadOrWriteAdministration e dos testes de login do SecurityIntegrationTest.
 */
@Timeout(120)
class LoginTest {

    private static Ambiente ambiente;
    private static Servidor servidor;

    @BeforeAll
    static void subir() throws Exception {
        ambiente = AmbienteDeTeste.novo(true);
        ContasDemonstracao.restaurar(ambiente);
        servidor = Servidor.iniciar(0, ambiente, RotasDeTesteConta.todas());
    }

    @AfterAll
    static void descer() {
        servidor.close();
    }

    @Test
    void asCincoContasEntramSemArquivoDeSenhasEOPerfilDecideAAdministracao() throws Exception {
        for (ContasDemonstracao.Demonstracao conta : ContasDemonstracao.TODAS) {
            Http http = new Http(servidor);
            HttpResponse<String> resposta = Entrar.com(http, conta.login(), conta.senha());
            assertEquals(303, resposta.statusCode(), conta.login());
            assertEquals("/admin", Entrar.destino(resposta), conta.login());
            HttpResponse<String> inicio = http.get("/admin");
            assertEquals(200, inicio.statusCode(), conta.login());
            assertTrue(inicio.body().contains(conta.nome()), conta.login());
            int esperado = conta.perfil() == Perfil.ADMIN ? 200 : 403;
            assertEquals(esperado, http.get("/admin/config/teste").statusCode(), conta.login());
        }
    }

    @Test
    void telaDeLoginAcessivelComOTokenEOsCamposCertos() throws Exception {
        String html = new Http(servidor).get("/login").body();
        assertTrue(html.indexOf("<a ") == html.indexOf("<a class=\"pular\" href=\"#conteudo\">"),
                "o primeiro link pula para o conteúdo");
        assertTrue(html.contains("<main id=\"conteudo\""));
        assertTrue(html.contains("<form method=\"post\" action=\"/login\""), html);
        assertTrue(html.contains("name=\"_csrf\""));
        assertTrue(html.contains("name=\"usuario\"") && html.contains("autocomplete=\"username\""));
        assertTrue(html.contains("name=\"senha\"") && html.contains("autocomplete=\"current-password\""));
        assertTrue(html.contains("<label for=\"usuario\">") && html.contains("id=\"usuario\""));
        assertTrue(html.contains("<label for=\"senha\">") && html.contains("id=\"senha\""));
        assertTrue(html.contains("src=\"/js/mostrar-senha.js\""));
        assertEquals(200, new Http(servidor).get("/js/mostrar-senha.js").statusCode());
    }

    @Test
    void senhaErradaOuUsuarioInexistenteDaoAMesmaMensagemEFicamNaAuditoria() throws Exception {
        int antes = Contas.auditoria(ambiente.banco()).size();
        for (String[] tentativa : new String[][]{{"milena", "errada-123"}, {"ninguem-aqui", "errada-123"}}) {
            Http http = new Http(servidor);
            HttpResponse<String> resposta = Entrar.com(http, tentativa[0], tentativa[1]);
            assertEquals(303, resposta.statusCode());
            assertEquals("/login?erro", Entrar.destino(resposta));
            String tela = http.get("/login?erro").body();
            assertTrue(tela.contains("Usuário ou senha incorretos."), tela);
            assertTrue(tela.contains("role=\"alert\""));
            assertEquals(303, http.get("/admin").statusCode(), "continua sem acesso");
        }
        List<String> auditoria = Contas.auditoria(ambiente.banco());
        assertEquals(List.of("LOGIN_NEGADO NEGADO null autenticacao", "LOGIN_NEGADO NEGADO null autenticacao"),
                auditoria.subList(antes, auditoria.size()));
    }

    @Test
    void loginAceitaMaiusculasAcentosEEspacosColados() throws Exception {
        Contas.Criada conta = Contas.criar(ambiente.banco(), Perfil.COLABORADOR);
        String digitado = "  " + conta.login().toUpperCase(Locale.ROOT).replace("TESTE", "TÉSTÉ") + " ";
        Http http = new Http(servidor);
        assertEquals("/admin", Entrar.destino(Entrar.com(http, digitado, Contas.SENHA)));
        assertEquals(200, http.get("/admin").statusCode());
    }

    @Test
    void contaDesativadaNaoEntra() throws Exception {
        Contas.Criada conta = Contas.criar(ambiente.banco(), Perfil.ADMIN);
        Contas.executar(ambiente.banco(), "update colaborador set ativo = false where id_colaborador = ?", conta.id());
        assertEquals("/login?erro", Entrar.destino(Entrar.com(new Http(servidor), conta.login(), Contas.SENHA)));
    }

    @Test
    void usuarioMuitoLongoERecusado() throws Exception {
        assertEquals(400, Entrar.com(new Http(servidor), "x".repeat(256), "qualquer").statusCode());
    }

    @Test
    void semLoginAAreaInternaLevaAoLogin() throws Exception {
        Http http = new Http(servidor);
        for (String caminho : new String[]{"/admin", "/admin/teste", "/admin/config/teste", "/conta/senha"}) {
            HttpResponse<String> resposta = http.get(caminho);
            assertEquals(303, resposta.statusCode(), caminho);
            assertEquals("/login", Entrar.destino(resposta), caminho);
        }
        assertEquals(200, http.get("/login").statusCode());
        assertEquals(200, http.get("/saude").statusCode(), "o que é público continua público");
    }

    @Test
    void colaboradorNaoLeNemGravaNaAdministracaoEATentativaEAuditada() throws Exception {
        Contas.Criada conta = Contas.criar(ambiente.banco(), Perfil.COLABORADOR);
        Http http = new Http(servidor);
        Entrar.com(http, conta.login(), Contas.SENHA);
        assertEquals(200, http.get("/admin/teste").statusCode(), "o resto da Área Interna é dos dois perfis");
        assertEquals(403, http.get("/admin/config/teste").statusCode());
        HttpResponse<String> gravar = Entrar.enviar(http, "/admin", "/admin/config/teste", "nome", "x");
        assertEquals(403, gravar.statusCode());
        assertTrue(gravar.body().contains("Acesso restrito"));
        List<String> auditoria = Contas.auditoria(ambiente.banco());
        assertTrue(auditoria.contains("ACESSO_NEGADO NEGADO " + conta.id() + " autenticacao"), auditoria
                .toString());
    }

    @Test
    void loginTrocaOCookieDaSessaoECookieSoPorHttpComSameSite() throws Exception {
        Http http = new Http(servidor);
        HttpResponse<String> tela = http.get("/login");
        String antes = http.sessao();
        assertNotNull(antes, "a tela de login cria a sessão por causa do token CSRF");
        String cookie = tela.headers().firstValue("Set-Cookie").orElse("");
        assertTrue(cookie.contains("HttpOnly"), cookie);
        assertTrue(cookie.contains("SameSite=Lax"), cookie);
        http.post("/login", Http.formulario("_csrf", Http.csrf(tela.body()), "usuario", "milena", "senha",
                "milena040"));
        assertNotEquals(antes, http.sessao(), "uma sessão nova depois do login (contra fixação de sessão)");
        assertEquals("1200", http.get("/admin/teste/prazo").body(), "20 minutos sem uso encerram a sessão");
    }

    @Test
    void sairEncerraASessaoEFicaNaAuditoria() throws Exception {
        Contas.Criada conta = Contas.criar(ambiente.banco(), Perfil.COLABORADOR);
        Http http = new Http(servidor);
        Entrar.com(http, conta.login(), Contas.SENHA);
        String inicio = http.get("/admin").body();
        assertTrue(inicio.contains("<form method=\"post\" action=\"/sair\""), inicio);
        HttpResponse<String> sair = http.post("/sair", Http.formulario("_csrf", Http.csrf(inicio)));
        assertEquals(303, sair.statusCode());
        assertEquals("/login?saiu", Entrar.destino(sair));
        assertTrue(http.get("/login?saiu").body().contains("Você saiu da Área Interna."));
        assertEquals(303, http.get("/admin").statusCode());
        assertTrue(Contas.auditoria(ambiente.banco()).contains("LOGOUT OK " + conta.id() + " autenticacao"));
    }

    @Test
    void quemJaEntrouVaiDiretoParaAAreaInterna() throws Exception {
        Http http = new Http(servidor);
        Entrar.com(http, "joaoe", "joaoe040");
        HttpResponse<String> login = http.get("/login");
        assertEquals(303, login.statusCode());
        assertEquals("/admin", Entrar.destino(login));
    }

    @Test
    void noModoLocalErrarASenhaVariasVezesNaoBloqueiaOLogin() throws Exception {
        Contas.Criada conta = Contas.criar(ambiente.banco(), Perfil.COLABORADOR);
        for (int i = 0; i < 12; i++) {
            assertEquals("/login?erro", Entrar.destino(Entrar.com(new Http(servidor), conta.login(), "errada")));
        }
        assertEquals("/admin", Entrar.destino(Entrar.com(new Http(servidor), conta.login(), Contas.SENHA)));
    }

    @Test
    void oLogNaoGuardaUsuarioNemSenha() throws Exception {
        try (CapturaDeLog log = new CapturaDeLog()) {
            Entrar.com(new Http(servidor), "usuario-secreto", "senha-secreta-123");
            Entrar.com(new Http(servidor), "guilherme", "guilherme040");
            String texto = log.aguardar("LOGIN_SENHA");
            assertTrue(texto.contains("event=AUDIT action=LOGIN_NEGADO target=autenticacao result=NEGADO"), texto);
            assertTrue(texto.contains("route=/login"), texto);
            for (String segredo : new String[]{"usuario-secreto", "senha-secreta", "guilherme", "JSESSIONID"}) {
                assertFalse(texto.contains(segredo), segredo);
            }
        }
    }
}

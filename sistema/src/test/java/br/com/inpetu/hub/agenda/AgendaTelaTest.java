package br.com.inpetu.hub.agenda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.AmbienteDeTeste;
import br.com.inpetu.hub.comum.Http;
import br.com.inpetu.hub.comum.Servidor;
import br.com.inpetu.hub.conta.Contas;
import br.com.inpetu.hub.conta.Entrar;
import br.com.inpetu.hub.conta.Perfil;

/** A tela pública da agenda (o calendário é montado pelo agenda.js a partir de /disponibilidade). */
@Timeout(120)
class AgendaTelaTest {

    private static Ambiente ambiente;
    private static Servidor servidor;

    @BeforeAll
    static void subir() throws Exception {
        ambiente = AmbienteDeTeste.novo();
        servidor = Servidor.iniciar(0, ambiente);
    }

    @AfterAll
    static void descer() {
        servidor.close();
    }

    @Test
    void agendaEPublicaNaoCriaSessaoETemOTituloOficialSemQuebraForcada() throws Exception {
        Http http = new Http(servidor);
        HttpResponse<String> resposta = http.get("/agenda");
        assertEquals(200, resposta.statusCode());
        assertTrue(resposta.headers().firstValue("Set-Cookie").isEmpty(), "a agenda não cria sessão");
        Matcher h1 = Pattern.compile("(?s)<h1[^>]*>(.*?)</h1>").matcher(resposta.body());
        assertTrue(h1.find());
        assertEquals("Agenda de Eventos, Visitações e Conexões", h1.group(1).strip());
        assertFalse(h1.group(1).contains("<br"), "o site oficial quebra o título pela largura");
    }

    @Test
    void calendarioAcessivelComOsLimitesDeDataDoRelogio() throws Exception {
        String html = new Http(servidor).get("/agenda").body();
        assertTrue(html.indexOf("<a ") == html.indexOf("<a class=\"pular\" href=\"#conteudo\">"));
        assertTrue(html.contains("role=\"group\" aria-label=\"Calendário de disponibilidade\""), html);
        assertTrue(html.contains("data-hoje=\"2026-11-12\"") && html.contains("data-ultimo-dia=\"2027-12-31\""));
        assertTrue(html.contains("aria-label=\"Mês anterior\"") && html.contains("aria-label=\"Próximo mês\""));
        assertTrue(html.contains("id=\"mes-titulo\" aria-live=\"polite\""));
        assertTrue(html.contains("id=\"detalhe-dia\" aria-live=\"polite\""));
        assertTrue(html.contains("<input id=\"filtro-dia\" type=\"date\" min=\"2026-01-01\" max=\"2027-12-31\""), html);
        assertTrue(html.contains("<label for=\"filtro-espaco\">") && html.contains("<label for=\"filtro-dia\">"));
        assertTrue(html.contains("src=\"/js/agenda.js\"") && html.contains("href=\"/css/agenda.css\""));
        assertEquals(200, new Http(servidor).get("/js/agenda.js").statusCode());
        assertEquals(200, new Http(servidor).get("/css/agenda.css").statusCode());
    }

    @Test
    void filtroListaSoOsEspacosReservaveisComOMobiliario() throws Exception {
        Contas.executar(ambiente.banco(), "insert into espaco(nome_espaco, capacidade, mobiliario, disponibilidade,"
                + " ativo) values ('Sala desativada', 5, null, true, false), ('Sala sem mobiliário', 7, null, true,"
                + " true)");
        String html = new Http(servidor).get("/agenda").body();
        assertTrue(html.contains("<option value=\"\">Todos os espaços</option>"));
        assertTrue(html.contains(">Sala Maker VIA · 32 cadeiras e 6 sofás</option>"), html);
        assertTrue(html.contains(">Sala sem mobiliário · até 7 pessoas</option>"), html);
        assertFalse(html.contains("Sala desativada"));
    }

    @Test
    void avisoDeCadastroDeEspacosSoParaQuemEstaLogado() throws Exception {
        assertFalse(new Http(servidor).get("/agenda").body().contains("id=\"agenda-aviso\""));
        Http equipe = new Http(servidor);
        Contas.Criada conta = Contas.criar(ambiente.banco(), Perfil.COLABORADOR);
        Entrar.com(equipe, conta.login(), Contas.SENHA);
        String html = equipe.get("/agenda").body();
        assertTrue(html.contains("id=\"agenda-aviso\""), html);
        assertTrue(html.contains("href=\"/admin/cadastros/espacos/editar\">Cadastrar espaço</a>"));
    }

    @Test
    void cabecalhoLevaAAgendaEAAreaInternaEOInicioAbreAAgenda() throws Exception {
        Http http = new Http(servidor);
        String html = http.get("/agenda").body();
        String topo = html.substring(html.indexOf("<header"), html.indexOf("</header>"));
        assertTrue(topo.contains("href=\"/agenda\" aria-current=\"page\""), topo);
        assertTrue(topo.contains("href=\"/admin\""), topo);
        HttpResponse<String> inicio = http.get("/");
        assertEquals(303, inicio.statusCode());
        assertEquals("/agenda", Entrar.destino(inicio));
    }
}

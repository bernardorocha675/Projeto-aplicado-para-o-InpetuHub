package br.com.inpetu.hub.espaco;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

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
import br.com.inpetu.hub.comum.log.CapturaDeLog;
import br.com.inpetu.hub.conta.Contas;
import br.com.inpetu.hub.conta.Entrar;
import br.com.inpetu.hub.conta.Perfil;

/** Cadastro de espaços com formulário próprio. Porta do EspacosCadastroIntegrationTest. */
@Timeout(120)
class EspacosCadastroTest {

    private static final String LISTA = "/admin/cadastros/espacos";
    private static final String FORMULARIO = "/admin/cadastros/espacos/editar";
    private static final String SALVAR = "/admin/cadastros/espacos/salvar";

    private static Ambiente ambiente;
    private static Servidor servidor;
    private static Http http;

    @BeforeAll
    static void subir() throws Exception {
        ambiente = AmbienteDeTeste.novo();
        servidor = Servidor.iniciar(0, ambiente);
        http = new Http(servidor);
        Contas.Criada conta = Contas.criar(ambiente.banco(), Perfil.COLABORADOR);
        assertEquals("/admin", Entrar.destino(Entrar.com(http, conta.login(), Contas.SENHA)));
    }

    @AfterAll
    static void descer() {
        servidor.close();
    }

    private static HttpResponse<String> salvar(String... campos) throws Exception {
        return Entrar.enviar(http, FORMULARIO, SALVAR, campos);
    }

    private static long quantos() throws Exception {
        return Long.parseLong(Contas.texto(ambiente.banco(), "select count(*) from espaco"));
    }

    private static String coluna(String coluna, long id) throws Exception {
        return Contas.texto(ambiente.banco(), "select " + coluna + " from espaco where id_espaco = ?", id);
    }

    private static long idPeloNome(String nome) throws Exception {
        return Long.parseLong(Contas.texto(ambiente.banco(), "select id_espaco from espaco where nome_espaco = ?",
                nome));
    }

    private static String campo(String html, String nome) {
        return Pattern.compile("<input[^>]*name=\"" + nome + "\"[^>]*>")
                .matcher(html)
                .results()
                .map(MatchResult::group)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Sem o campo " + nome + ": " + html));
    }

    @Test
    void listaMostraOsEspacosDoHubPorNomeComSituacao() throws Exception {
        String html = http.get(LISTA).body();
        assertTrue(html.indexOf("Auditório") < html.indexOf("Coworking"), "em ordem de nome");
        assertTrue(html.contains("Sala Maker VIA") && html.contains("32 cadeiras e 6 sofás"), html);
        assertTrue(html.contains("href=\"" + FORMULARIO + "\""), "botão de cadastrar");
        assertTrue(html.contains("href=\"" + FORMULARIO + "?id=" + idPeloNome("Coworking") + "\""), "Editar");
        assertTrue(html.contains("<th scope=\"col\">Disponível para reservas</th>"), html);
        assertTrue(html.contains(">Sim<"), "Sim/Não em vez de true/false");
        assertFalse(html.contains(">true<") || html.contains(">false<"));
        assertTrue(http.get("/admin").body().contains("href=\"" + LISTA + "\""), "o início leva aos espaços");
    }

    @Test
    void formularioExigeInteiroPositivoENome() throws Exception {
        String html = http.get(FORMULARIO).body();
        String capacidade = campo(html, "capacidade");
        for (String atributo : new String[]{"type=\"number\"", "step=\"1\"", "min=\"1\"", "max=\"2147483647\"",
                "required"}) {
            assertTrue(capacidade.contains(atributo), capacidade);
        }
        String nome = campo(html, "nome");
        assertTrue(nome.contains("required") && nome.contains("maxlength=\"255\""), nome);
        assertTrue(html.contains("<label for=\"capacidade\">"), html);
        assertTrue(html.contains("Salvar"), html);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.89", "1.5", "1,5", "2147483648", "abc", ""})
    void capacidadeInvalidaNaoGravaEDevolveOFormularioPreenchido(String capacidade) throws Exception {
        long antes = quantos();
        HttpResponse<String> resposta = salvar("nome", "Sala de teste", "capacidade", capacidade, "disponivel", "on");
        assertEquals(400, resposta.statusCode(), capacidade);
        String html = resposta.body();
        assertTrue(html.contains("Capacidade: informe um número inteiro de 1 a 2147483647."), html);
        assertTrue(html.contains("role=\"alert\""));
        assertTrue(campo(html, "nome").contains("value=\"Sala de teste\""), "o nome digitado continua");
        assertTrue(campo(html, "capacidade").contains("value=\"" + capacidade + "\""), "a capacidade também");
        assertTrue(campo(html, "capacidade").contains("aria-invalid=\"true\""));
        assertTrue(campo(html, "disponivel").contains("checked"), "a caixa marcada continua marcada");
        assertFalse(campo(html, "ativo").contains("checked"), "e a desmarcada, desmarcada");
        assertEquals(antes, quantos(), "nada foi gravado");
    }

    @Test
    void nomeVazioOuLongoDemaisERecusado() throws Exception {
        assertTrue(salvar("nome", "  ", "capacidade", "10").body().contains("Nome do espaço: preencha este campo."));
        assertEquals(400, salvar("nome", "x".repeat(256), "capacidade", "10").statusCode());
    }

    @Test
    void depoisDoErroCorrigeESalvaSemReabrirOCadastro() throws Exception {
        assertEquals(400, salvar("nome", "Nova sala", "capacidade", "0").statusCode());
        HttpResponse<String> salvo = salvar("nome", "  Nova sala  ", "capacidade", "25", "mobiliario",
                "25 cadeiras", "ativo", "on", "disponivel", "on");
        assertEquals(303, salvo.statusCode());
        assertEquals(LISTA + "?salvo", Entrar.destino(salvo));
        long id = idPeloNome("Nova sala");
        assertEquals("25", coluna("capacidade", id));
        assertEquals("25 cadeiras", coluna("mobiliario", id));
        assertEquals("TRUE", coluna("ativo", id));
        assertEquals("TRUE", coluna("disponibilidade", id));
        assertTrue(http.get(LISTA + "?salvo").body().contains("Espaço salvo."));
        List<String> auditoria = Contas.auditoria(ambiente.banco());
        assertTrue(auditoria.stream().anyMatch(l -> l.startsWith("CADASTRO_CRIADO OK ") && l.endsWith(" espacos")),
                auditoria.toString());
    }

    @Test
    void edicaoInvalidaPreservaOBancoEACorrecaoGrava() throws Exception {
        salvar("nome", "Original", "capacidade", "10", "ativo", "on", "disponivel", "on");
        long id = idPeloNome("Original");
        String tela = FORMULARIO + "?id=" + id;
        assertTrue(campo(http.get(tela).body(), "nome").contains("value=\"Original\""));
        HttpResponse<String> invalida = Entrar.enviar(http, tela, SALVAR, "id", String.valueOf(id), "nome",
                "Alterada", "capacidade", "1.5");
        assertEquals(400, invalida.statusCode());
        assertTrue(invalida.body().contains("name=\"id\" value=\"" + id + "\""), "continua editando o mesmo");
        assertEquals("Original", coluna("nome_espaco", id));
        assertEquals("10", coluna("capacidade", id));
        HttpResponse<String> certa = Entrar.enviar(http, tela, SALVAR, "id", String.valueOf(id), "nome", "Alterada",
                "capacidade", "1");
        assertEquals(303, certa.statusCode());
        assertEquals("Alterada", coluna("nome_espaco", id));
        assertEquals("1", coluna("capacidade", id));
        assertEquals("FALSE", coluna("ativo", id), "caixa desmarcada grava Não");
        assertEquals("FALSE", coluna("disponibilidade", id));
        assertTrue(Contas.auditoria(ambiente.banco()).stream().anyMatch(l -> l.startsWith("CADASTRO_ALTERADO OK ")));
    }

    @Test
    void espacoInexistenteDa404() throws Exception {
        assertEquals(404, http.get(FORMULARIO + "?id=999999").statusCode());
        assertEquals(404, http.get(FORMULARIO + "?id=abc").statusCode());
        assertEquals(404, Entrar.enviar(http, FORMULARIO, SALVAR, "id", "999999", "nome", "X", "capacidade", "3")
                .statusCode());
    }

    @Test
    void excluirSemHistoricoApagaEComHistoricoRecusaSemApagar() throws Exception {
        salvar("nome", "Para excluir", "capacidade", "4");
        long livre = idPeloNome("Para excluir");
        HttpResponse<String> excluido = Entrar.enviar(http, LISTA, "/admin/cadastros/espacos/excluir", "id", String
                .valueOf(livre));
        assertEquals(303, excluido.statusCode());
        assertEquals(LISTA + "?excluido", Entrar.destino(excluido));
        assertEquals(null, coluna("nome_espaco", livre));
        assertTrue(Contas.auditoria(ambiente.banco()).stream().anyMatch(l -> l.startsWith("CADASTRO_EXCLUIDO OK ")));

        long comHistorico = idPeloNome("Auditório");
        Contas.executar(ambiente.banco(), "insert into empreendedor(nome, email, ativo) values ('Pessoa', 'p@x.com',"
                + " true)");
        Contas.executar(ambiente.banco(), "insert into reserva(id_empreendedor, id_espaco, persona_id, data,"
                + " hora_inicio, hora_fim, status_reserva, numero_participantes, inscricao_paga, financiado, academico,"
                + " usa_cota, percentual_sinal, percentual_locacao, percentual_operacao, percentual_limpeza,"
                + " taxas_configuradas) select max(id_empreendedor), ?, 1, '2026-12-01', '08:00', '12:00', 'APROVADA',"
                + " 10, false, false, false, false, 0, 100, 0, 0, false from empreendedor", comHistorico);
        HttpResponse<String> recusado = Entrar.enviar(http, LISTA, "/admin/cadastros/espacos/excluir", "id", String
                .valueOf(comHistorico));
        assertEquals(409, recusado.statusCode());
        assertTrue(recusado.body().contains("desative o cadastro"), recusado.body());
        assertEquals("Auditório", coluna("nome_espaco", comHistorico));
    }

    @Test
    void erroDeValidacaoVaiAoLogSemCopiarOFormulario() throws Exception {
        try (CapturaDeLog log = new CapturaDeLog()) {
            salvar("nome", "conteudo-privado-nao-publicar", "capacidade", "0");
            String texto = log.aguardar("CADASTRO_INVALIDO");
            assertTrue(texto.contains("event=AUDIT action=CADASTRO_INVALIDO target=espacos result=NEGADO"), texto);
            assertFalse(texto.contains("conteudo-privado-nao-publicar"));
        }
    }

    @Test
    void semLoginOCadastroLevaAoLogin() throws Exception {
        Http anonimo = new Http(servidor);
        assertEquals("/login", Entrar.destino(anonimo.get(LISTA)));
        assertEquals("/login", Entrar.destino(anonimo.get(FORMULARIO)));
    }
}

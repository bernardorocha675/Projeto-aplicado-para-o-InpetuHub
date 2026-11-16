package br.com.inpetu.hub.agenda;

import static br.com.inpetu.hub.comum.LeitorJson.lista;
import static br.com.inpetu.hub.comum.LeitorJson.objeto;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.AmbienteDeTeste;
import br.com.inpetu.hub.comum.Http;
import br.com.inpetu.hub.comum.LeitorJson;
import br.com.inpetu.hub.comum.Servidor;
import br.com.inpetu.hub.conta.Contas;

/**
 * {@code /disponibilidade}: só reservas aprovadas ocupam, espaço inativo não é agenda ocupada e a consulta não cria
 * sessão. Porta dos testes de disponibilidade do FluxosIntegrationTest e do PublicDemoLoginTest.
 */
@Timeout(120)
class DisponibilidadeTest {

    private interface Teste {

        void rodar(Ambiente ambiente, Http http) throws Exception;
    }

    private static void com(Teste teste) throws Exception {
        Ambiente ambiente = AmbienteDeTeste.novo();
        try (Servidor servidor = Servidor.iniciar(0, ambiente)) {
            teste.rodar(ambiente, new Http(servidor));
        }
    }

    private static List<Object> dias(Http http, String consulta) throws Exception {
        HttpResponse<String> resposta = http.get("/disponibilidade?" + consulta);
        assertEquals(200, resposta.statusCode(), resposta.body());
        return lista(objeto(LeitorJson.ler(resposta.body())).get("dias"));
    }

    private static Map<String, Object> dia(Http http, String consulta) throws Exception {
        List<Object> dias = dias(http, consulta);
        assertEquals(1, dias.size());
        return objeto(dias.getFirst());
    }

    private static Map<String, Object> sala(Map<String, Object> dia, String nome) {
        return lista(dia.get("espacos")).stream()
                .map(e -> objeto(e))
                .filter(e -> nome.equals(e.get("nome")))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void bancoNovoTemOsDezEspacosLivresNoMesInteiro() throws Exception {
        com((ambiente, http) -> {
            HttpResponse<String> resposta = http.get("/disponibilidade?mes=2026-10");
            assertTrue(resposta.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
            Map<String, Object> json = objeto(LeitorJson.ler(resposta.body()));
            assertEquals("08:00", json.get("abertura"));
            assertEquals("20:00", json.get("fechamento"));
            List<Object> dias = lista(json.get("dias"));
            assertEquals(31, dias.size());
            for (Object item : dias) {
                assertEquals("DISPONÍVEL", objeto(item).get("estado"));
            }
            Map<String, Object> primeiro = objeto(dias.getFirst());
            assertEquals("2026-10-01", primeiro.get("data"));
            assertEquals(10, lista(primeiro.get("espacos")).size());
            Map<String, Object> maker = sala(primeiro, "Sala Maker VIA");
            assertEquals(32L, maker.get("capacidade"));
            assertEquals("32 cadeiras e 6 sofás", maker.get("mobiliario"));
            assertEquals(List.of(Map.of("inicio", "08:00", "fim", "20:00")), maker.get("livres"));
            assertEquals(List.of(), maker.get("ocupados"));
        });
    }

    @Test
    void espacoInativoOuIndisponivelNaoSignificaAgendaOcupada() throws Exception {
        com((ambiente, http) -> {
            Contas.executar(ambiente.banco(), "update espaco set ativo = false");
            Map<String, Object> inativos = dia(http, "data=2026-10-20");
            assertEquals("SEM_ESPACOS", inativos.get("estado"));
            assertEquals(List.of(), inativos.get("espacos"));
            Contas.executar(ambiente.banco(), "update espaco set ativo = true, disponibilidade = false");
            assertEquals("SEM_ESPACOS", dia(http, "data=2026-10-20").get("estado"));
        });
    }

    @Test
    void filtroSemEspacoElegivelNaoMarcaOcupacao() throws Exception {
        com((ambiente, http) -> assertEquals("SEM_ESPACOS", dia(http, "data=2026-10-20&espacoId=9223372036854775807")
                .get("estado")));
    }

    @Test
    void reservaAprovadaParcialMantemHorariosLivresEAPendenteNaoOcupa() throws Exception {
        com((ambiente, http) -> {
            long auditorio = Reservas.espaco(ambiente.banco(), "Auditório");
            Reservas.gravar(ambiente.banco(), auditorio, "2026-10-20", "10:00", "12:00", "APROVADA", "Encontro");
            Reservas.gravar(ambiente.banco(), auditorio, "2026-10-20", "14:00", "16:00", "PENDENTE", "Em análise");
            Map<String, Object> dia = dia(http, "data=2026-10-20");
            assertEquals("PARCIAL", dia.get("estado"));
            Map<String, Object> sala = sala(dia, "Auditório");
            assertEquals(List.of(Map.of("inicio", "10:00", "fim", "12:00", "titulo", "Encontro", "status",
                    "APROVADA")), sala.get("ocupados"));
            assertEquals(List.of(Map.of("inicio", "08:00", "fim", "10:00"), Map.of("inicio", "12:00", "fim",
                    "20:00")), sala.get("livres"));
        });
    }

    @Test
    void reservaAprovadaDoDiaInteiroDeixaOEspacoSemHorario() throws Exception {
        com((ambiente, http) -> {
            long sala = Reservas.espaco(ambiente.banco(), "Coworking");
            Reservas.gravar(ambiente.banco(), sala, "2026-10-21", "08:00", "20:00", "APROVADA", "Dia todo");
            Map<String, Object> filtrado = dia(http, "data=2026-10-21&espacoId=" + sala);
            assertEquals("INDISPONÍVEL", filtrado.get("estado"));
            assertEquals(List.of(), sala(filtrado, "Coworking").get("livres"));
            assertEquals("PARCIAL", dia(http, "data=2026-10-21").get("estado"), "os outros espaços continuam livres");
            Contas.executar(ambiente.banco(), "update reserva set status_reserva = 'CANCELADA'");
            assertEquals("DISPONÍVEL", dia(http, "data=2026-10-21&espacoId=" + sala).get("estado"),
                    "cancelada libera o horário");
        });
    }

    @Test
    void feriadoAtivoVemMarcado() throws Exception {
        com((ambiente, http) -> {
            assertEquals(true, dia(http, "data=2026-11-15").get("feriado"));
            assertEquals(false, dia(http, "data=2026-11-16").get("feriado"));
            Contas.executar(ambiente.banco(), "update feriado set ativo = false where data = '2026-11-15'");
            assertEquals(false, dia(http, "data=2026-11-15").get("feriado"));
        });
    }

    @Test
    void tituloComAspasETagsSaiEscapado() throws Exception {
        com((ambiente, http) -> {
            long sala = Reservas.espaco(ambiente.banco(), "Coworking");
            String titulo = "Fala \"nova\" \\ <script>alert(1)</script> & cia\nlinha";
            Reservas.gravar(ambiente.banco(), sala, "2026-10-22", "08:00", "09:00", "APROVADA", titulo);
            String corpo = http.get("/disponibilidade?data=2026-10-22&espacoId=" + sala).body();
            assertFalse(corpo.contains("<script>"), corpo);
            Map<String, Object> dia = objeto(lista(objeto(LeitorJson.ler(corpo)).get("dias")).getFirst());
            assertEquals(titulo, objeto(lista(sala(dia, "Coworking").get("ocupados")).getFirst()).get("titulo"));
        });
    }

    @Test
    void consultaSemCookieNaoCriaSessao() throws Exception {
        com((ambiente, http) -> {
            HttpResponse<String> resposta = http.get("/disponibilidade?mes=2026-10");
            assertTrue(resposta.headers().firstValue("Set-Cookie").isEmpty(), "a consulta criou uma sessão");
            assertEquals(null, http.sessao());
        });
    }

    @Test
    void parametrosInvalidosSaoRecusados() throws Exception {
        com((ambiente, http) -> {
            for (String consulta : new String[]{"mes=abc", "mes=2026-13", "data=2026-02-30", "data=ontem",
                    "espacoId=x", "mes=1999-01", "mes=2101-01"}) {
                assertEquals(400, http.get("/disponibilidade?" + consulta).statusCode(), consulta);
            }
        });
    }

    @Test
    void semMesUsaOMesAtualDoRelogio() throws Exception {
        com((ambiente, http) -> {
            List<Object> dias = dias(http, "");
            assertEquals(30, dias.size());
            assertEquals("2026-11-01", objeto(dias.getFirst()).get("data"));
        });
    }

    @Test
    void consultaTemLimitePorMinuto() throws Exception {
        AmbienteDeTeste.Relogio relogio = new AmbienteDeTeste.Relogio();
        Ambiente ambiente = AmbienteDeTeste.novo(false, relogio);
        try (Servidor servidor = Servidor.iniciar(0, ambiente)) {
            Http http = new Http(servidor);
            for (int i = 0; i < 120; i++) {
                assertEquals(200, http.get("/disponibilidade?data=2026-10-20").statusCode(), "consulta " + i);
            }
            HttpResponse<String> bloqueada = http.get("/disponibilidade?data=2026-10-20");
            assertEquals(429, bloqueada.statusCode());
            assertEquals("60", bloqueada.headers().firstValue("Retry-After").orElse(""));
            relogio.adiantar(Duration.ofSeconds(61));
            assertEquals(200, http.get("/disponibilidade?data=2026-10-20").statusCode());
        }
    }
}

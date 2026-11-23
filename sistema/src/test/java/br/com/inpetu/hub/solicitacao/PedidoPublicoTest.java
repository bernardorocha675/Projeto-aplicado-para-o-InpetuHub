package br.com.inpetu.hub.solicitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.AmbienteDeTeste;
import br.com.inpetu.hub.comum.Http;
import br.com.inpetu.hub.comum.Servidor;
import br.com.inpetu.hub.comum.log.CapturaDeLog;
import br.com.inpetu.hub.conta.Contas;
import br.com.inpetu.hub.conta.Entrar;

/**
 * O pedido público: formulário, resumo com o orçamento, envio sem reservar nem criar cadastro e a página de recebida
 * com a conferência manual. Porta do RelatorioBugsIntegrationTest e dos testes de pedido do FluxosIntegrationTest. O
 * relógio dos testes está em 12/11/2026, 10h.
 */
@Timeout(120)
class PedidoPublicoTest {

    private interface Teste {

        void rodar(Ambiente ambiente, Http http, long sala) throws Exception;
    }

    /** Um servidor por teste: os envios de /reservas têm limite por endereço. */
    private static void com(Teste teste) throws Exception {
        Ambiente ambiente = AmbienteDeTeste.novo();
        try (Servidor servidor = Servidor.iniciar(0, ambiente)) {
            long sala = Long.parseLong(Contas.texto(ambiente.banco(),
                    "select id_espaco from espaco where nome_espaco = 'Sala Maker VIA'"));
            teste.rodar(ambiente, new Http(servidor), sala);
        }
    }

    private static String campo(String html, String id) {
        Matcher m = Pattern.compile("<(input|select|textarea)[^>]*id=\"" + id + "\"[^>]*>").matcher(html);
        assertTrue(m.find(), "sem o campo " + id);
        return m.group();
    }

    private static long contar(Ambiente ambiente, String tabela) throws Exception {
        return Long.parseLong(Contas.texto(ambiente.banco(), "select count(*) from " + tabela));
    }

    @Test
    void formularioVemPreenchidoPelaAgendaComOsLimitesDeData() throws Exception {
        com((ambiente, http, sala) -> {
            HttpResponse<String> resposta = http.get("/reserva?data=2026-11-24&espacoId=" + sala
                    + "&horaInicio=09:00&horaFim=10:00");
            assertEquals(200, resposta.statusCode());
            String html = resposta.body();
            assertTrue(campo(html, "data").contains("value=\"2026-11-24\""), "data ISO");
            assertTrue(campo(html, "data").contains("min=\"2026-11-12\"") && campo(html, "data").contains(
                    "max=\"2027-12-31\""));
            assertTrue(campo(html, "horaInicio").contains("value=\"09:00\""));
            assertTrue(html.contains("<option value=\"" + sala + "\" selected>Sala Maker VIA · 32 cadeiras e 6 sofás"),
                    html);
            assertTrue(html.contains(">P6 — Externos</option>"), html);
            assertTrue(html.indexOf("<a ") == html.indexOf("<a class=\"pular\""), "primeiro link é o de pular");
            assertTrue(html.contains("name=\"_csrf\""));
        });
    }

    @Test
    void formularioPedeAoNavegadorParaPreencherOContatoETemRotulos() throws Exception {
        com((ambiente, http, sala) -> {
            String html = http.get("/reserva").body();
            String[][] campos = {{"nome", "name"}, {"email", "email"}, {"telefone", "tel"}, {"empresa", "organization"},
                    {"cargo", "organization-title"}};
            for (String[] c : campos) {
                assertTrue(campo(html, c[0]).contains("autocomplete=\"" + c[1] + "\""), campo(html, c[0]));
                assertTrue(html.contains("<label for=\"" + c[0] + "\">"), c[0]);
            }
            assertTrue(campo(html, "objetivo").contains("maxlength=\"2000\""));
            assertTrue(html.contains("<option value=\"NENHUM\">Sem documento</option>"));
        });
    }

    @ParameterizedTest
    @CsvSource({"nome,123134142", "nome,João 123", "email,11231231@a", "email,joao@example..com",
            "telefone,123123adwdawd", "telefone,123"})
    void contatoInvalidoVoltaAoFormularioSemGravar(String nome, String valor) throws Exception {
        com((ambiente, http, sala) -> {
            Map<String, String> c = Pedidos.campos(sala);
            c.put(nome, valor);
            HttpResponse<String> resposta = Pedidos.resumo(http, c);
            assertEquals(400, resposta.statusCode());
            assertTrue(campo(resposta.body(), nome).contains("aria-invalid=\"true\""));
            assertTrue(campo(resposta.body(), "data").contains("value=\"2026-11-24\""), "a data continua");
            assertEquals(0, contar(ambiente, "solicitacao_publica"));
        });
    }

    @Test
    void errosFicamLigadosAosCamposEOResumoLevaAteEles() throws Exception {
        com((ambiente, http, sala) -> {
            Map<String, String> c = Pedidos.campos(sala);
            c.put("nome", "João 123");
            c.put("email", "11231231@a");
            String html = Pedidos.resumo(http, c).body();
            for (String id : new String[]{"nome", "email"}) {
                assertTrue(campo(html, id).contains("aria-describedby=\"erro-" + id + "\""), campo(html, id));
            }
            assertTrue(
                    html.contains(
                            "<p id=\"erro-nome\" class=\"erro-campo\">Informe um nome com letras, sem números.</p>"),
                    html);
            assertTrue(html.contains("<a href=\"#nome\">Nome: Informe um nome com letras, sem números.</a>"));
            assertTrue(html.contains(
                    "<a href=\"#email\">Email: Informe um email completo, como nome@instituicao.com.br.</a>"));
            assertTrue(html.contains("role=\"alert\""));
            assertFalse(campo(html, "titulo").contains("aria-invalid"), "campo certo não fica marcado");
            assertTrue(campo(html, "nome").contains("value=\"João 123\""), "o digitado volta");

            c = Pedidos.campos(sala);
            c.put("data", "134421-03-12");
            String data = Pedidos.resumo(http, c).body();
            assertTrue(data.contains("Data: Confira a data, o horário ou o número informado."));
            assertFalse(data.contains("java.time") || data.contains("Failed to convert"));
            assertTrue(data.contains("joao@example.com"), "os outros campos continuam");
        });
    }

    @Test
    void regraRecusadaApareceNoTopoComOsCamposPreenchidos() throws Exception {
        com((ambiente, http, sala) -> {
            Map<String, String> c = Pedidos.campos(sala);
            c.put("data", "2027-01-20");
            HttpResponse<String> semPreco = Pedidos.resumo(http, c);
            assertEquals(400, semPreco.statusCode());
            assertTrue(semPreco.body().contains("Tabela de preços não configurada para 2027 / 1 turno(s)."), semPreco
                    .body());
            assertTrue(campo(semPreco.body(), "data").contains("value=\"2027-01-20\""));
            c.put("data", "2026-11-12");
            assertTrue(Pedidos.resumo(http, c).body().contains("Não é possível reservar no passado."));
        });
    }

    @Test
    void resumoMostraOOrcamentoEOsDadosConferidos() throws Exception {
        com((ambiente, http, sala) -> {
            Map<String, String> c = Pedidos.campos(sala);
            c.put("empresa", "");
            c.put("tipoDocumento", "CPF");
            c.put("documento", "123.456.789-09");
            HttpResponse<String> resposta = Pedidos.resumo(http, c);
            assertEquals(200, resposta.statusCode(), resposta.body());
            String html = resposta.body();
            for (String trecho : new String[]{"Confira sua solicitação", "Sala Maker VIA", "24/11/2026 · 08:00–12:00",
                    "4 horas / 1 turno", "P6 — Externos", "Não informada", "CPF 12345678909", "(48) 99999-1234",
                    "R$ 1.600,00", "30%", "R$ 480,00", "R$ 1.120,00", "não bloqueia a agenda"}) {
                assertTrue(html.contains(trecho), trecho + " ausente: " + html);
            }
            assertTrue(html.contains("<form method=\"post\" action=\"/reservas/solicitar\""));
            assertTrue(html.contains("href=\"/reserva?editar=true\""));
        });
    }

    @Test
    void voltarParaEditarTrazOQueFoiDigitado() throws Exception {
        com((ambiente, http, sala) -> {
            Pedidos.resumo(http, Pedidos.campos(sala));
            String html = http.get("/reserva?editar=true").body();
            assertTrue(campo(html, "nome").contains("value=\"João D’Ávila\""), campo(html, "nome"));
            assertTrue(campo(html, "data").contains("value=\"2026-11-24\""));
            assertTrue(html.contains("joao@example.com"));
        });
    }

    @Test
    void envioGravaSoOPedidoSemReservaCotaNemCadastro() throws Exception {
        com((ambiente, http, sala) -> {
            HttpResponse<String> resumo = Pedidos.resumo(http, Pedidos.campos(sala));
            HttpResponse<String> envio = Pedidos.confirmar(http, resumo.body());
            assertEquals(303, envio.statusCode());
            assertEquals("/reservas/recebida", Entrar.destino(envio));
            assertEquals(1, contar(ambiente, "solicitacao_publica"));
            assertEquals("RECEBIDA", Contas.texto(ambiente.banco(), "select estado from solicitacao_publica"));
            assertEquals("12345678000190", Contas.texto(ambiente.banco(), "select documento from solicitacao_publica"));
            assertEquals("2026-11-13 10:00:00", Contas.texto(ambiente.banco(),
                    "select formatdatetime(expira_em, 'yyyy-MM-dd HH:mm:ss') from solicitacao_publica"));
            assertEquals(0, contar(ambiente, "empreendedor"), "o cadastro só nasce quando a equipe aceita");
            assertEquals(0, contar(ambiente, "reserva"));
            assertEquals(0, contar(ambiente, "movimentacao_cota"));
            List<String> auditoria = Contas.auditoria(ambiente.banco());
            assertTrue(auditoria.contains("SOLICITACAO_RECEBIDA OK null solicitacao"), auditoria.toString());

            String recebida = http.get("/reservas/recebida").body();
            assertTrue(recebida.contains("Solicitação recebida"));
            assertTrue(recebida.contains("conferência manual"), recebida);
            assertTrue(recebida.contains("não bloqueia a agenda"));
            assertFalse(recebida.contains("name=\"code\""), "sem código de email na v1");
            assertTrue(recebida.contains("href=\"/agenda\">Voltar à agenda</a>"));

            HttpResponse<String> deNovo = http.post("/reservas/solicitar", Http.formulario("_csrf", Http.csrf(http.get(
                    "/reserva").body())));
            assertEquals(400, deNovo.statusCode(), "o rascunho já foi usado");
            assertTrue(deNovo.body().contains("Preencha o formulário antes de confirmar."));
            assertEquals(1, contar(ambiente, "solicitacao_publica"));
        });
    }

    @Test
    void semPedidoNesteNavegadorAPaginaExplica() throws Exception {
        com((ambiente, http, sala) -> {
            String html = http.get("/reservas/recebida").body();
            assertTrue(html.contains("Nenhuma solicitação foi enviada neste navegador."), html);
        });
    }

    @Test
    void enviosTemLimitePorEndereco() throws Exception {
        com((ambiente, http, sala) -> {
            Map<String, String> c = Pedidos.campos(sala);
            c.put("nome", "123");
            int ultima = 0;
            for (int i = 0; i < 16; i++) {
                ultima = Pedidos.resumo(http, c).statusCode();
                if (i < 15) {
                    assertEquals(400, ultima, "envio " + (i + 1));
                }
            }
            assertEquals(429, ultima);
        });
    }

    @Test
    void logNaoGuardaOsDadosDoFormulario() throws Exception {
        com((ambiente, http, sala) -> {
            try (CapturaDeLog log = new CapturaDeLog()) {
                Map<String, String> c = Pedidos.campos(sala);
                c.put("email", "privado@exemplo-secreto.com");
                c.put("titulo", "Evento sigiloso");
                Pedidos.confirmar(http, Pedidos.resumo(http, c).body());
                c.put("nome", "Nome 123 sigiloso");
                Pedidos.resumo(http, c);
                String texto = log.aguardar("PEDIDO_INVALIDO");
                assertTrue(texto.contains("action=SOLICITACAO_RECEBIDA"), texto);
                assertTrue(texto.contains("route=/reservas/resumo"), texto);
                for (String segredo : new String[]{"privado@", "sigiloso", "D’Ávila", "12345678000190"}) {
                    assertFalse(texto.contains(segredo), segredo);
                }
            }
        });
    }

    @Test
    void logDizOsCamposRecusadosEARegraSemOQueFoiDigitado() throws Exception {
        com((ambiente, http, sala) -> {
            try (CapturaDeLog log = new CapturaDeLog()) {
                Map<String, String> c = Pedidos.campos(sala);
                c.put("nome", "Nome 123 sigiloso");
                c.put("email", "sigiloso@");
                Pedidos.resumo(http, c);
                assertTrue(log.aguardar("FORM_REJECTED").contains(
                        "event=FORM_REJECTED form=solicitacao fields=nome,email"), log.texto());
                c = Pedidos.campos(sala);
                c.put("horaFim", "07:00");
                c.put("horaInicio", "08:00");
                Pedidos.resumo(http, c);
                String texto = log.aguardar("RULE_REJECTED");
                assertTrue(
                        texto.matches("(?s).*event=RULE_REJECTED form=solicitacao rule=[A-Za-z]+\\.[a-zA-Z]+:[0-9]+.*"),
                        texto);
                assertFalse(texto.contains("sigiloso"), texto);
            }
        });
    }

    /** Achado da alpha.1: os atributos saíam colados (requiredmaxlength) e o navegador os ignorava. */
    @Test
    void atributosDosCamposSaemSeparados() throws Exception {
        com((ambiente, http, sala) -> {
            String tela = http.get("/reserva").body();
            for (String campo : new String[]{"data", "nome", "email", "telefone", "cidade", "titulo",
                    "numeroParticipantes"}) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("<input id=\"" + campo + "\"[^>]*>")
                        .matcher(
                                tela);
                assertTrue(m.find(), campo);
                assertTrue(m.group().matches("(?s)<input( [a-z-]+(=\"[^\"]*\")?)+ */>"), m.group());
                assertTrue(m.group().contains(" required "), m.group());
            }
            assertTrue(tela.contains(" maxlength=\"20\""), "limite do telefone");
            assertTrue(tela.contains(" step=\"1\""), "participantes inteiros");
        });
    }
}

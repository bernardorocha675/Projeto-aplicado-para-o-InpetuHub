package br.com.inpetu.hub.interna;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.AmbienteDeTeste;
import br.com.inpetu.hub.comum.Http;
import br.com.inpetu.hub.comum.Servidor;
import br.com.inpetu.hub.conta.Contas;
import br.com.inpetu.hub.conta.Entrar;
import br.com.inpetu.hub.conta.Perfil;

/**
 * A Área Interna pelas telas: painel, menu, solicitações, lista e detalhe das reservas e as ações pelos formulários,
 * até a agenda pública mostrar o horário ocupado (a fatia de ponta a ponta).
 */
@Timeout(120)
class AreaInternaTelasTest {

    private interface Teste {

        void rodar(Ambiente ambiente, Servidor servidor, Http http) throws Exception;
    }

    private static void com(Perfil perfil, Teste teste) throws Exception {
        Ambiente ambiente = AmbienteDeTeste.novo();
        Cenario.taxasZeradas(ambiente);
        try (Servidor servidor = Servidor.iniciar(0, ambiente)) {
            Http http = new Http(servidor);
            Contas.Criada conta = Contas.criar(ambiente.banco(), perfil);
            assertEquals("/admin", Entrar.destino(Entrar.com(http, conta.login(), Contas.SENHA)));
            teste.rodar(ambiente, servidor, http);
        }
    }

    private static HttpResponse<String> enviar(Http http, String tela, String destino, String... campos)
            throws Exception {
        return Entrar.enviar(http, tela, destino, campos);
    }

    private static long reservaDoPedido(Ambiente ambiente, long pedido) throws Exception {
        return Long.parseLong(Contas.texto(ambiente.banco(), "select reserva_id from solicitacao_publica where id = ?",
                pedido));
    }

    @Test
    void painelMostraOsNumerosEOMenuInternoLevaATudo() throws Exception {
        com(Perfil.COLABORADOR, (ambiente, servidor, http) -> {
            Cenario.pedido(ambiente, Cenario.campos(ambiente));
            String html = http.get("/admin").body();
            assertTrue(html.contains("<h1>Painel</h1>"), html);
            assertTrue(html.contains("Olá, Pessoa de teste"));
            String menu = html.substring(html.indexOf("<nav class=\"menu-interno\""), html.indexOf("</nav>", html
                    .indexOf("<nav class=\"menu-interno\"")));
            for (String destino : new String[]{"/admin", "/admin/reservas", "/admin/solicitacoes",
                    "/admin/cadastros/espacos", "/conta/senha"}) {
                assertTrue(menu.contains("href=\"" + destino + "\""), destino + " fora do menu: " + menu);
            }
            assertTrue(menu.contains("href=\"/admin\" aria-current=\"page\""), "item atual marcado");
            assertTrue(menu.contains("aguardando análise"), "aviso de solicitações pendentes");
            assertTrue(html.contains("<form method=\"post\" action=\"/sair\""));
            assertTrue(html.contains("Solicitações recebidas") && html.contains(">1<"), html);
        });
    }

    @Test
    void cabecalhoERodapeOficiaisSoComLinksDoSiteEmNovaAba() throws Exception {
        com(Perfil.COLABORADOR, (ambiente, servidor, http) -> {
            for (String caminho : new String[]{"/agenda", "/admin", "/reserva", "/login"}) {
                Http quem = caminho.equals("/admin") ? http : new Http(servidor);
                String html = quem.get(caminho).body();
                String topo = html.substring(html.indexOf("<header"), html.indexOf("</header>"));
                for (String local : new String[]{"href=\"/\"", "href=\"/agenda\"", "href=\"/admin\""}) {
                    assertTrue(topo.contains(local), caminho + " sem " + local);
                }
                Matcher externos = Pattern.compile("<a href=\"(https?://[^\"]+)\"([^>]*)>").matcher(html);
                int quantos = 0;
                while (externos.find()) {
                    quantos++;
                    String endereco = externos.group(1);
                    assertTrue(endereco.startsWith("https://inpetuhub.sites.ufsc.br/") || endereco.matches(
                            "https://(www\\.)?(facebook|instagram|youtube)\\.com/.*"), caminho + ": " + endereco);
                    assertTrue(externos.group(2).contains("target=\"_blank\"") && externos.group(2).contains(
                            "rel=\"noopener\""), caminho + ": " + externos.group());
                }
                assertTrue(quantos >= 6, caminho + " sem os links oficiais");
                assertFalse(html.contains("inpetuhub.sites.ufsc.br/sobre") || html.contains("termos-de-uso"),
                        "links quebrados do site");
                assertTrue(html.contains("Sapiens Parque") && html.contains("aria-label=\"Instagram\""), caminho);
            }
        });
    }

    @Test
    void aceitarPeloFormularioLevaAReservaERecusarTiraDaLista() throws Exception {
        com(Perfil.COLABORADOR, (ambiente, servidor, http) -> {
            long pedido = Cenario.pedido(ambiente, Cenario.campos(ambiente));
            Map<String, String> outro = Cenario.campos(ambiente);
            outro.put("titulo", "Pedido para recusar");
            long recusar = Cenario.pedido(ambiente, outro);
            String lista = http.get("/admin/solicitacoes").body();
            assertTrue(lista.contains("Encontro de teste") && lista.contains("Pedido para recusar"), lista);
            assertTrue(lista.contains("Maria Souza · Instituição · CNPJ 12345678000190"), lista);
            assertTrue(lista.contains("não bloqueia a agenda"));

            HttpResponse<String> semConferir = enviar(http, "/admin/solicitacoes", "/admin/solicitacoes/aceitar", "id",
                    String.valueOf(pedido), "como", "Telefone");
            assertEquals(400, semConferir.statusCode());
            assertTrue(semConferir.body().contains("Confira a identidade"), semConferir.body());

            HttpResponse<String> aceito = enviar(http, "/admin/solicitacoes", "/admin/solicitacoes/aceitar", "id",
                    String.valueOf(pedido), "identidade", "sim", "como", "Confirmado por telefone com a coordenação.");
            assertEquals(303, aceito.statusCode());
            long reserva = reservaDoPedido(ambiente, pedido);
            assertEquals("/admin/reservas/" + reserva + "?ok=aceita", Entrar.destino(aceito));
            assertTrue(http.get(Entrar.destino(aceito)).body().contains("Solicitação encaminhada"));

            HttpResponse<String> recusado = enviar(http, "/admin/solicitacoes", "/admin/solicitacoes/recusar", "id",
                    String.valueOf(recusar));
            assertEquals("/admin/solicitacoes?ok=recusada", Entrar.destino(recusado));
            String depois = http.get("/admin/solicitacoes?ok=recusada").body();
            assertFalse(depois.contains("Pedido para recusar"));
            assertTrue(depois.contains("Solicitação recusada."));
            assertTrue(depois.contains("Nenhuma solicitação aguardando análise."));
        });
    }

    @Test
    void detalheMostraSimNaoOsValoresEOHistorico() throws Exception {
        com(Perfil.COLABORADOR, (ambiente, servidor, http) -> {
            Map<String, String> c = Cenario.campos(ambiente);
            c.put("academico", "on");
            long pedido = Cenario.pedido(ambiente, c);
            enviar(http, "/admin/solicitacoes", "/admin/solicitacoes/aceitar", "id", String.valueOf(pedido),
                    "identidade", "sim", "como", "ok");
            long reserva = reservaDoPedido(ambiente, pedido);
            String html = http.get("/admin/reservas/" + reserva).body();
            for (String trecho : new String[]{"Encontro de teste", "Pendente", "Maria Souza", "CNPJ 12345678000190",
                    "Sala Maker VIA", "24/11/2026 · 08:00–12:00", "P6 — Externos", "Não / Não / Sim", "R$ 1.600,00",
                    "30% · R$ 480,00", "SOLICITAÇÃO", "Aprovar reserva", "Recusar", "Cancelar reserva",
                    "Salvar e recalcular"}) {
                assertTrue(html.contains(trecho), trecho + " ausente");
            }
            String principal = html.substring(html.indexOf("<main"), html.indexOf("</main>"));
            assertFalse(principal.contains("false") || principal.contains("true") || principal.contains("null"),
                    "sem valores crus");
            assertFalse(html.contains("Força maior"), "força maior só aparece para o gestor");
            assertEquals(404, http.get("/admin/reservas/999999").statusCode());
            assertEquals(404, http.get("/admin/reservas/abc").statusCode());
        });
    }

    @Test
    void aprovarPeloDetalheOcupaAAgendaPublicaECancelarLibera() throws Exception {
        com(Perfil.ADMIN, (ambiente, servidor, http) -> {
            long pedido = Cenario.pedido(ambiente, Cenario.campos(ambiente));
            enviar(http, "/admin/solicitacoes", "/admin/solicitacoes/aceitar", "id", String.valueOf(pedido),
                    "identidade", "sim", "como", "ok");
            long reserva = reservaDoPedido(ambiente, pedido);
            String detalhe = "/admin/reservas/" + reserva;
            String sala = String.valueOf(Cenario.espaco(ambiente, "Sala Maker VIA"));
            String agenda = "/disponibilidade?data=2026-11-24&espacoId=" + sala;
            assertTrue(new Http(servidor).get(agenda).body().contains("\"DISPONÍVEL\""), "pendente não ocupa");

            HttpResponse<String> aprovada = enviar(http, detalhe, detalhe + "/aprovar");
            assertEquals(detalhe + "?ok=aprovada", Entrar.destino(aprovada));
            assertTrue(http.get(detalhe + "?ok=aprovada").body().contains("Reserva aprovada."));
            String ocupada = new Http(servidor).get(agenda).body();
            assertTrue(ocupada.contains("\"PARCIAL\"") && ocupada.contains("Encontro de teste"), ocupada);

            String comGestor = http.get(detalhe).body();
            assertTrue(comGestor.contains("Força maior"), "o gestor vê a exceção");
            assertTrue(comGestor.contains("Registrar pagamento"));
            enviar(http, detalhe, detalhe + "/pagamento", "situacao", "SINAL_PAGO");
            assertTrue(http.get(detalhe).body().contains("Sinal pago"));
            HttpResponse<String> cancelar = enviar(http, detalhe, detalhe + "/cancelar", "justificativa",
                    "Pedido do solicitante");
            assertEquals(detalhe + "?ok=cancelada", Entrar.destino(cancelar));
            assertTrue(new Http(servidor).get(agenda).body().contains("\"DISPONÍVEL\""), "cancelada libera");
        });
    }

    @Test
    void recusarAlterarECancelarPeloDetalhe() throws Exception {
        com(Perfil.COLABORADOR, (ambiente, servidor, http) -> {
            long primeiro = Cenario.pedido(ambiente, Cenario.campos(ambiente));
            enviar(http, "/admin/solicitacoes", "/admin/solicitacoes/aceitar", "id", String.valueOf(primeiro),
                    "identidade", "sim", "como", "ok");
            String detalhe = "/admin/reservas/" + reservaDoPedido(ambiente, primeiro);
            assertEquals(400, enviar(http, detalhe, detalhe + "/recusar", "motivo", "").statusCode());
            assertEquals(detalhe + "?ok=recusada", Entrar.destino(enviar(http, detalhe, detalhe + "/recusar", "motivo",
                    "Documentação incompleta")));
            String recusada = http.get(detalhe).body();
            assertTrue(recusada.contains("Recusada") && recusada.contains("Documentação incompleta"));
            assertFalse(recusada.contains("Aprovar reserva"), "sem ações para recusada");

            long segundo = Cenario.pedido(ambiente, Cenario.campos(ambiente));
            enviar(http, "/admin/solicitacoes", "/admin/solicitacoes/aceitar", "id", String.valueOf(segundo),
                    "identidade", "sim", "como", "ok");
            String outro = "/admin/reservas/" + reservaDoPedido(ambiente, segundo);
            HttpResponse<String> alterada = enviar(http, outro, outro + "/alterar", "data", "2026-11-29", "horaInicio",
                    "08:00", "horaFim", "12:00", "espacoId", String.valueOf(Cenario.espaco(ambiente, "Coworking")),
                    "personaId", String.valueOf(Cenario.persona(ambiente, "P6")));
            assertEquals(outro + "?ok=alterada", Entrar.destino(alterada));
            assertTrue(http.get(outro).body().contains("60% · R$ 960,00"), "domingo recalculado");
            assertEquals(400, enviar(http, outro, outro + "/alterar", "data", "ontem", "horaInicio", "08:00", "horaFim",
                    "12:00", "espacoId", "1", "personaId", "1").statusCode());
            HttpResponse<String> forca = enviar(http, outro, outro + "/cancelar", "forcaMaior", "sim", "justificativa",
                    "x");
            assertEquals(400, forca.statusCode(), "colaborador não usa força maior");
            assertEquals(outro + "?ok=cancelada", Entrar.destino(enviar(http, outro, outro + "/cancelar",
                    "justificativa", "Pedido do solicitante")));
            assertTrue(http.get(outro).body().contains("Cancelada"));
        });
    }

    @Test
    void listaFiltraPorSituacaoEspacoEBusca() throws Exception {
        com(Perfil.COLABORADOR, (ambiente, servidor, http) -> {
            long a = Cenario.pedido(ambiente, Cenario.campos(ambiente));
            Map<String, String> c = Cenario.campos(ambiente);
            c.put("nome", "Carlos Lima");
            c.put("tipoDocumento", "NENHUM");
            c.put("documento", "");
            c.put("email", "carlos@example.com");
            c.put("espacoId", String.valueOf(Cenario.espaco(ambiente, "Coworking")));
            long b = Cenario.pedido(ambiente, c);
            for (long pedido : new long[]{a, b}) {
                enviar(http, "/admin/solicitacoes", "/admin/solicitacoes/aceitar", "id", String.valueOf(pedido),
                        "identidade", "sim", "como", "ok");
            }
            String todas = http.get("/admin/reservas").body();
            assertTrue(todas.contains("Maria Souza") && todas.contains("Carlos Lima"));
            assertTrue(todas.contains("<th scope=\"col\">Situação</th>"));
            String busca = http.get("/admin/reservas?busca=carlos").body();
            assertTrue(busca.contains("Carlos Lima") && !busca.contains("Maria Souza"));
            assertTrue(http.get("/admin/reservas?busca=12345678000190").body().contains("Maria Souza"));
            String espaco = http.get("/admin/reservas?espaco=" + Cenario.espaco(ambiente, "Coworking")).body();
            assertTrue(espaco.contains("Carlos Lima") && !espaco.contains("Maria Souza"));
            assertTrue(http.get("/admin/reservas?status=APROVADA").body().contains(
                    "Nenhuma reserva encontrada."));
            assertFalse(http.get("/admin/reservas?busca=null").body().contains("Carlos Lima"), "sem “null” na busca");
            assertEquals(400, http.get("/admin/reservas?status=QUALQUER").statusCode());
        });
    }
}

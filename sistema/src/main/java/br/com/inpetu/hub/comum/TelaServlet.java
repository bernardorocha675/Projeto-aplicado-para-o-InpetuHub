package br.com.inpetu.hub.comum;

import java.io.IOException;
import java.io.Serial;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.com.inpetu.hub.comum.log.Eventos;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * {@code GET /tela}: a página avisa o que aconteceu nela (clique, campo que o navegador recusou, erro de script). Só
 * entram códigos destas listas; nada digitado. Não grava dados nem cria sessão. O cabeçalho próprio impede que outro
 * site use o endereço, porque o navegador não deixa mandá-lo de fora sem permissão (CORS).
 */
final class TelaServlet extends HttpServlet {

    private static final Set<String> CLIQUES = Set.of("DIA", "MES_ANTERIOR", "MES_PROXIMO", "FILTRO_ESPACO",
            "FILTRO_DIA", "HORARIO", "CONFIRMAR_SIM", "CONFIRMAR_NAO", "LINK_EXTERNO");
    private static final Set<String> FALHAS = Set.of("AGENDA");
    private static final Set<String> SCRIPTS = Set.of("agenda.js", "confirmar.js", "mostrar-senha.js", "tela.js",
            "OUTRO_SCRIPT");
    private static final Set<String> CAMPOS = Set.of("data", "espacoId", "horaInicio", "horaFim", "nome",
            "tipoDocumento", "documento", "empresa", "email", "telefone", "cidade", "segmento", "cargo", "titulo",
            "personaId", "numeroParticipantes", "objetivo", "observacoes", "inscricaoPaga", "financiado", "academico",
            "usuario", "senha", "atual", "nova", "capacidade", "mobiliario", "disponivel", "ativo", "identidade",
            "como", "motivo", "justificativa", "forcaMaior", "situacao", "status", "espaco", "persona", "busca", "id",
            "OUTRO_CAMPO");
    private static final Map<String, Set<String>> ALVOS = Map.of("CLIQUE", CLIQUES, "FALHA", FALHAS, "ERRO_SCRIPT",
            SCRIPTS, "CAMPO_INVALIDO", CAMPOS);
    private static final int MAXIMO_DE_CAMPOS = 20;
    private static final int POR_MINUTO = 120;
    private static final Duration JANELA = Duration.ofMinutes(1);

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Limites limites;

    TelaServlet(Ambiente ambiente) {
        this.limites = new Limites(ambiente.relogio());
    }

    /** Se o código está em alguma das listas (os testes conferem as telas contra elas). */
    static boolean conhecido(String codigo) {
        return ALVOS.containsKey(codigo) || ALVOS.values().stream().anyMatch(lista -> lista.contains(codigo));
    }

    @Override
    protected void doGet(HttpServletRequest pedido, HttpServletResponse resposta) throws IOException {
        if (!"1".equals(pedido.getHeader("X-Inpetu-Tela"))) {
            resposta.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!limites.permite("tela:" + Limites.resumo(pedido.getRemoteAddr()), POR_MINUTO, JANELA)) {
            pedido.setAttribute(ErroServlet.ESPERA, JANELA.toSeconds());
            resposta.sendError(429);
            return;
        }
        String evento = pedido.getParameter("evento");
        String alvo = pedido.getParameter("alvo");
        if (evento == null || alvo == null) {
            resposta.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        // Os campos recusados vêm juntos, num aviso só.
        boolean campos = "CAMPO_INVALIDO".equals(evento);
        List<String> alvos = campos ? List.of(alvo.split(",", MAXIMO_DE_CAMPOS + 1)) : List.of(alvo);
        if (alvos.size() > MAXIMO_DE_CAMPOS || !ALVOS.getOrDefault(evento, Set.of()).containsAll(alvos)) {
            resposta.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        String pagina = pedido.getParameter("pagina");
        if (campos) {
            Eventos.camposRecusados(alvos, pagina);
        } else {
            Eventos.tela(evento, alvo, pagina, linha(pedido.getParameter("linha")));
        }
        resposta.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private static int linha(String texto) {
        return texto != null && texto.matches("[0-9]{1,5}") ? Integer.parseInt(texto) : 0;
    }
}

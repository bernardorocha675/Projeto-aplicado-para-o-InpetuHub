package br.com.inpetu.hub.interna;

import java.io.IOException;
import java.io.Serial;
import java.util.List;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.Formatos;
import br.com.inpetu.hub.comum.RegraException;
import br.com.inpetu.hub.comum.Telas;
import br.com.inpetu.hub.conta.ContaSessao;
import br.com.inpetu.hub.empreendedor.Documentos;
import br.com.inpetu.hub.solicitacao.PedidoConferido;
import br.com.inpetu.hub.solicitacao.SolicitacaoServico;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Pedidos públicos aguardando análise: aceitar (com a conferência manual) ou recusar. */
final class SolicitacoesServlet extends HttpServlet {

    static final String RAIZ = "/admin/solicitacoes";

    @Serial
    private static final long serialVersionUID = 1L;

    /** Um pedido pronto para a tela. */
    public record Cartao(long id, String titulo, String quem, String contato, String quando, String espaco,
            String persona, String objetivo, String expira) {
    }

    private final transient Ambiente ambiente;
    private final transient SolicitacaoServico servico;

    SolicitacoesServlet(Ambiente ambiente) {
        this.ambiente = ambiente;
        this.servico = new SolicitacaoServico(ambiente);
    }

    @Override
    protected void doGet(HttpServletRequest pedido, HttpServletResponse resposta) throws ServletException,
            IOException {
        if (!Telas.caminho(pedido).equals(RAIZ)) {
            resposta.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        List<Cartao> cartoes = servico.recebidos().stream().map(r -> {
            PedidoConferido p = r.pedido();
            String quem = p.nome() + (p.empresa() == null ? "" : " · " + p.empresa()) + " · " + Documentos.exibir(p
                    .tipoDocumento(), p.documento());
            return new Cartao(r.id(), p.titulo(), quem, p.email() + " · " + p.telefone(), Formatos.dia(p.data()) + " · "
                    + p.inicio() + "–" + p.fim(), r.espaco(), r.persona(), p.objetivo(),
                    Formatos.diaHora(r
                            .expiraEm()));
        }).toList();
        MenuInterno.preparar(pedido, ambiente, "solicitacoes");
        pedido.setAttribute("cartoes", cartoes);
        if ("recusada".equals(pedido.getParameter("ok"))) {
            pedido.setAttribute("aviso", "Solicitação recusada.");
        }
        Telas.mostrar(pedido, resposta, "interna/solicitacoes");
    }

    @Override
    protected void doPost(HttpServletRequest pedido, HttpServletResponse resposta) {
        long colaborador = ContaSessao.doPedido(pedido).id();
        long id = Numeros.inteiro(pedido.getParameter("id"));
        switch (Telas.caminho(pedido)) {
            case RAIZ + "/aceitar" -> {
                long reserva = servico.aceitar(id, colaborador, "sim".equals(pedido.getParameter("identidade")), pedido
                        .getParameter("como"));
                Telas.redirecionar(resposta, ReservasServlet.RAIZ + "/" + reserva + "?ok=aceita");
            }
            case RAIZ + "/recusar" -> {
                servico.recusar(id, colaborador);
                Telas.redirecionar(resposta, RAIZ + "?ok=recusada");
            }
            default -> throw new RegraException("Ação desconhecida.");
        }
    }
}

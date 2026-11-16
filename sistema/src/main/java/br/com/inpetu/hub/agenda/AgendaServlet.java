package br.com.inpetu.hub.agenda;

import java.io.IOException;
import java.io.Serial;
import java.time.LocalDate;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.Telas;
import br.com.inpetu.hub.conta.ContaSessao;
import br.com.inpetu.hub.espaco.EspacoRepositorio;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A agenda pública. As novas reservas vão de hoje até 31/12 do ano seguinte; a consulta começa em 1º/1 deste ano. A
 * tela não cria sessão (não tem formulário que mude dados).
 */
final class AgendaServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Ambiente ambiente;
    private final transient EspacoRepositorio espacos;

    AgendaServlet(Ambiente ambiente) {
        this.ambiente = ambiente;
        this.espacos = new EspacoRepositorio(ambiente.banco());
    }

    @Override
    protected void doGet(HttpServletRequest pedido, HttpServletResponse resposta) throws ServletException,
            IOException {
        LocalDate hoje = LocalDate.now(ambiente.relogio());
        pedido.setAttribute("hoje", hoje.toString());
        pedido.setAttribute("primeiroDia", LocalDate.of(hoje.getYear(), 1, 1).toString());
        pedido.setAttribute("ultimoDia", LocalDate.of(hoje.getYear() + 1, 12, 31).toString());
        pedido.setAttribute("espacos", espacos.reservaveis());
        pedido.setAttribute("equipeLogada", ContaSessao.doPedido(pedido) != null);
        pedido.setAttribute("paginaAtual", "agenda");
        Telas.mostrar(pedido, resposta, "agenda/agenda");
    }
}

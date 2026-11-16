package br.com.inpetu.hub.interna;

import java.io.IOException;
import java.io.Serial;
import java.sql.Date;
import java.time.LocalDate;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.Telas;
import br.com.inpetu.hub.reserva.ReservaConsultas;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** O painel da Área Interna: pedidos e reservas pendentes, reservas de hoje e as próximas. */
final class PainelServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Ambiente ambiente;
    private final transient ReservaConsultas consultas;

    PainelServlet(Ambiente ambiente) {
        this.ambiente = ambiente;
        this.consultas = new ReservaConsultas(ambiente.banco());
    }

    @Override
    protected void doGet(HttpServletRequest pedido, HttpServletResponse resposta) throws ServletException,
            IOException {
        LocalDate hoje = LocalDate.now(ambiente.relogio());
        MenuInterno.preparar(pedido, ambiente, "painel");
        pedido.setAttribute("reservasPendentes", consultas.contar(
                "select count(*) from reserva where status_reserva = 'PENDENTE'"));
        pedido.setAttribute("reservasHoje", consultas.contar("select count(*) from reserva where data = ? and"
                + " status_reserva in ('PENDENTE', 'APROVADA')", Date.valueOf(hoje)));
        pedido.setAttribute("proximas", Linhas.de(consultas.proximas(hoje, 8)));
        Telas.mostrar(pedido, resposta, "interna/painel");
    }
}

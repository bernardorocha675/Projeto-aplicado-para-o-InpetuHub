package br.com.inpetu.hub.agenda;

import java.io.IOException;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.ErroServlet;
import br.com.inpetu.hub.comum.Json;
import br.com.inpetu.hub.comum.Limites;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * {@code GET /disponibilidade?mes=AAAA-MM} (ou {@code data=AAAA-MM-DD}) e, opcional, {@code espacoId}. Público, não
 * cria sessão e tem limite de 120 consultas por minuto por endereço, como no antigo.
 */
final class DisponibilidadeServlet extends HttpServlet {

    private static final int PRIMEIRO_ANO = 2000;
    private static final int ULTIMO_ANO = 2100;
    private static final Duration JANELA = Duration.ofMinutes(1);

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Ambiente ambiente;
    private final transient Disponibilidade disponibilidade;
    private final transient Limites limites;

    DisponibilidadeServlet(Ambiente ambiente) {
        this.ambiente = ambiente;
        this.disponibilidade = new Disponibilidade(ambiente.banco());
        this.limites = new Limites(ambiente.relogio());
    }

    @Override
    protected void doGet(HttpServletRequest pedido, HttpServletResponse resposta) throws IOException {
        if (!limites.permite("agenda:" + Limites.resumo(pedido.getRemoteAddr()), 120, JANELA)) {
            pedido.setAttribute(ErroServlet.ESPERA, JANELA.toSeconds());
            resposta.sendError(429);
            return;
        }
        LocalDate inicio;
        LocalDate fim;
        Long espacoId;
        try {
            String data = pedido.getParameter("data");
            String mes = pedido.getParameter("mes");
            if (data != null) {
                inicio = LocalDate.parse(data);
                fim = inicio;
            } else {
                YearMonth ano = mes == null ? YearMonth.now(ambiente.relogio()) : YearMonth.parse(mes);
                inicio = ano.atDay(1);
                fim = ano.atEndOfMonth();
            }
            String espaco = pedido.getParameter("espacoId");
            espacoId = espaco == null || espaco.isEmpty() ? null : Long.valueOf(espaco);
        } catch (DateTimeException | NumberFormatException invalido) {
            resposta.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (inicio.getYear() < PRIMEIRO_ANO || fim.getYear() > ULTIMO_ANO) {
            resposta.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        byte[] corpo = Json.escrever(disponibilidade.consultar(inicio, fim, espacoId)).getBytes(
                StandardCharsets.UTF_8);
        resposta.setContentType("application/json;charset=UTF-8");
        resposta.setContentLength(corpo.length);
        resposta.getOutputStream().write(corpo);
    }
}

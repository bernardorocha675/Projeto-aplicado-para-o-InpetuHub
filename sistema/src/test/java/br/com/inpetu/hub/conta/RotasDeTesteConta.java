package br.com.inpetu.hub.conta;

import java.io.IOException;
import java.io.Serial;
import java.util.Map;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Rotas que só existem nos testes: uma tela da administração e uma que mostra o prazo da sessão. */
final class RotasDeTesteConta {

    private RotasDeTesteConta() {
    }

    static Map<String, HttpServlet> todas() {
        return Map.of("/admin/config/teste", new Texto(), "/admin/teste", new Texto(), "/admin/teste/prazo",
                new Prazo());
    }

    static final class Texto extends HttpServlet {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest pedido, HttpServletResponse resposta) throws IOException {
            resposta.setContentType("text/plain;charset=UTF-8");
            resposta.getWriter().write("ok");
        }

        @Override
        protected void doPost(HttpServletRequest pedido, HttpServletResponse resposta) throws IOException {
            doGet(pedido, resposta);
        }
    }

    static final class Prazo extends HttpServlet {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest pedido, HttpServletResponse resposta) throws IOException {
            resposta.setContentType("text/plain;charset=UTF-8");
            resposta.getWriter().write(String.valueOf(pedido.getSession(false).getMaxInactiveInterval()));
        }
    }
}

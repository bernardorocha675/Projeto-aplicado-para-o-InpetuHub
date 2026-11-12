package br.com.inpetu.hub.comum;

import java.io.IOException;
import java.io.Serial;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Map;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Rotas que só existem nos testes: falhas de propósito e um formulário com o token CSRF. */
final class RotasDeTeste {

    static final String SEGREDO = "sql-password-private@example.com";

    private RotasDeTeste() {
    }

    static Map<String, HttpServlet> todas() {
        return Map.of("/teste/falha", new Falha(), "/teste/regra", new Regra(), "/teste/conflito", new Conflito(),
                "/teste/formulario", new Formulario());
    }

    static final class Falha extends HttpServlet {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest pedido, HttpServletResponse resposta) {
            throw new IllegalStateException(SEGREDO);
        }
    }

    static final class Regra extends HttpServlet {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest pedido, HttpServletResponse resposta) {
            throw new RegraException("Confira o horário.");
        }
    }

    static final class Conflito extends HttpServlet {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest pedido, HttpServletResponse resposta) throws IOException {
            throw new IOException(new SQLIntegrityConstraintViolationException(SEGREDO));
        }
    }

    /** GET devolve o token CSRF da sessão; POST só chega aqui se o token conferir. */
    static final class Formulario extends HttpServlet {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest pedido, HttpServletResponse resposta) throws IOException {
            resposta.setContentType("text/plain;charset=UTF-8");
            resposta.getWriter().write(String.valueOf(pedido.getAttribute(CsrfFilter.ATRIBUTO)));
        }

        @Override
        protected void doPost(HttpServletRequest pedido, HttpServletResponse resposta) throws IOException {
            resposta.setContentType("text/plain;charset=UTF-8");
            resposta.getWriter().write("gravado");
        }
    }
}

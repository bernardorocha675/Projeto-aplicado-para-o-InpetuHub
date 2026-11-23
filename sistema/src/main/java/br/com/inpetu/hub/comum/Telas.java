package br.com.inpetu.hub.comum;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Apoio dos servlets: mostrar uma tela JSP e redirecionar depois de um formulário. */
public final class Telas {

    private Telas() {
    }

    /** Mostra {@code WEB-INF/jsp/<tela>.jsp}. */
    public static void mostrar(HttpServletRequest pedido, HttpServletResponse resposta, String tela)
            throws ServletException, IOException {
        pedido.getRequestDispatcher("/WEB-INF/jsp/" + tela + ".jsp").forward(pedido, resposta);
    }

    /** 303: o navegador abre o destino com GET (o formulário não é reenviado ao recarregar). */
    public static void redirecionar(HttpServletResponse resposta, String destino) {
        resposta.setStatus(HttpServletResponse.SC_SEE_OTHER);
        resposta.setHeader("Location", destino);
    }

    /** O caminho dentro do sistema, sem parâmetros (o que os filtros comparam). */
    public static String caminho(HttpServletRequest pedido) {
        String info = pedido.getPathInfo();
        return pedido.getServletPath() + (info == null ? "" : info);
    }
}

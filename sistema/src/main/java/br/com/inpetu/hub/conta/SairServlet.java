package br.com.inpetu.hub.conta;

import java.io.Serial;

import br.com.inpetu.hub.auditoria.Auditoria;
import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.Telas;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/** Sair (só por POST, com o token CSRF): encerra a sessão e registra na auditoria. */
final class SairServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Auditoria auditoria;

    SairServlet(Ambiente ambiente) {
        this.auditoria = new Auditoria(ambiente.banco(), ambiente.relogio());
    }

    @Override
    protected void doPost(HttpServletRequest pedido, HttpServletResponse resposta) {
        HttpSession sessao = pedido.getSession(false);
        if (sessao != null) {
            if (sessao.getAttribute(SessaoFilter.NA_SESSAO) instanceof ContaSessao conta) {
                auditoria.seguranca(conta.id(), "LOGOUT", "OK");
            }
            sessao.invalidate();
        }
        Telas.redirecionar(resposta, "/login?saiu");
    }
}

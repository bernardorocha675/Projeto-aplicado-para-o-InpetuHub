package br.com.inpetu.hub.conta;

import java.io.IOException;
import java.io.Serial;
import java.util.Optional;

import br.com.inpetu.hub.auditoria.Auditoria;
import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.Telas;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Troca de senha da própria conta. Encerra todas as sessões dela (a versão sobe). */
final class SenhaServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Ambiente ambiente;
    private final transient ContaRepositorio contas;
    private final transient Auditoria auditoria;

    SenhaServlet(Ambiente ambiente) {
        this.ambiente = ambiente;
        this.contas = new ContaRepositorio(ambiente.banco());
        this.auditoria = new Auditoria(ambiente.banco(), ambiente.relogio());
    }

    @Override
    protected void doGet(HttpServletRequest pedido, HttpServletResponse resposta) throws ServletException,
            IOException {
        mostrar(pedido, resposta, null);
    }

    @Override
    protected void doPost(HttpServletRequest pedido, HttpServletResponse resposta) throws ServletException,
            IOException {
        ContaSessao logada = (ContaSessao) pedido.getAttribute(SessaoFilter.NO_PEDIDO);
        Optional<Conta> encontrada = contas.porId(logada.id());
        if (encontrada.isEmpty()) {
            Telas.redirecionar(resposta, "/login?expirou");
            return;
        }
        Conta conta = encontrada.get();
        String atual = pedido.getParameter("atual");
        String nova = pedido.getParameter("nova");
        String problema;
        if (!Senhas.confere(atual, conta.hash())) {
            problema = "A senha atual não confere.";
        } else {
            problema = Senhas.problemaDaNova(nova);
            if (problema == null && Senhas.confere(nova, conta.hash())) {
                problema = "A nova senha precisa ser diferente da atual.";
            }
        }
        if (problema != null) {
            resposta.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mostrar(pedido, resposta, problema);
            return;
        }
        if (!contas.trocarSenha(conta, Senhas.hash(nova))) {
            pedido.getSession().invalidate();
            Telas.redirecionar(resposta, "/login?expirou");
            return;
        }
        auditoria.registrar(conta.id(), "SENHA_ALTERADA", "colaboradores", conta.id(), "OK", null);
        pedido.getSession().invalidate();
        Telas.redirecionar(resposta, "/login?alterada");
    }

    private void mostrar(HttpServletRequest pedido, HttpServletResponse resposta, String erro)
            throws ServletException, IOException {
        pedido.setAttribute("erro", erro);
        pedido.setAttribute("modoLocal", ambiente.modoLocal());
        Telas.mostrar(pedido, resposta, "conta/senha");
    }
}

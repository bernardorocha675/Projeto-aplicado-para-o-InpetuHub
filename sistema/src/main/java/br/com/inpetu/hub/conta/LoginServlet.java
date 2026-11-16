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
import jakarta.servlet.http.HttpSession;

/**
 * Login por formulário. Usuário ou senha errados, conta inexistente ou desativada dão a mesma mensagem (e o mesmo tempo
 * de resposta). No sucesso, a sessão antiga é descartada e nasce outra, contra fixação de sessão.
 */
final class LoginServlet extends HttpServlet {

    private static final int MAXIMO_USUARIO = 255;
    private static final int MAXIMO_SENHA = 1024;

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Ambiente ambiente;
    private final transient ContaRepositorio contas;
    private final transient Auditoria auditoria;

    LoginServlet(Ambiente ambiente) {
        this.ambiente = ambiente;
        this.contas = new ContaRepositorio(ambiente.banco());
        this.auditoria = new Auditoria(ambiente.banco(), ambiente.relogio());
    }

    @Override
    protected void doGet(HttpServletRequest pedido, HttpServletResponse resposta) throws ServletException,
            IOException {
        HttpSession sessao = pedido.getSession(false);
        if (sessao != null && sessao.getAttribute(SessaoFilter.NA_SESSAO) != null) {
            Telas.redirecionar(resposta, "/admin");
            return;
        }
        aviso(pedido);
        Telas.mostrar(pedido, resposta, "conta/login");
    }

    @Override
    protected void doPost(HttpServletRequest pedido, HttpServletResponse resposta) throws IOException {
        String digitado = pedido.getParameter("usuario");
        String senha = pedido.getParameter("senha");
        if (digitado == null || senha == null || digitado.length() > MAXIMO_USUARIO || senha
                .length() > MAXIMO_SENHA) {
            resposta.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        Optional<Conta> encontrada = contas.porLogin(NomesDeLogin.normalizar(digitado));
        boolean certa;
        if (encontrada.isPresent()) {
            certa = Senhas.confere(senha, encontrada.get().hash()) && encontrada.get().ativa();
        } else {
            Senhas.conferirNada(senha);
            certa = false;
        }
        if (!certa) {
            auditoria.seguranca(null, "LOGIN_NEGADO", "NEGADO");
            Telas.redirecionar(resposta, "/login?erro");
            return;
        }
        Conta conta = encontrada.get();
        HttpSession antiga = pedido.getSession(false);
        if (antiga != null) {
            antiga.invalidate();
        }
        pedido.getSession(true).setAttribute(SessaoFilter.NA_SESSAO, ContaSessao.de(conta, ambiente.relogio()
                .millis()));
        auditoria.seguranca(conta.id(), "LOGIN_SENHA", "OK");
        Telas.redirecionar(resposta, "/admin");
    }

    /** A mensagem da tela vem só de palavras conhecidas no endereço, nunca de texto livre. */
    private static void aviso(HttpServletRequest pedido) {
        String[] aviso = null;
        if (pedido.getParameter("erro") != null) {
            aviso = new String[]{"erro", "Usuário ou senha incorretos. Confira os dados e tente novamente."};
        } else if (pedido.getParameter("expirou") != null) {
            aviso = new String[]{"aviso", "Sua sessão terminou. Entre de novo para continuar."};
        } else if (pedido.getParameter("saiu") != null) {
            aviso = new String[]{"sucesso", "Você saiu da Área Interna."};
        } else if (pedido.getParameter("alterada") != null) {
            aviso = new String[]{"sucesso", "Senha alterada. Entre com a nova senha."};
        }
        if (aviso != null) {
            pedido.setAttribute("tipoAviso", aviso[0]);
            pedido.setAttribute("aviso", aviso[1]);
        }
    }
}

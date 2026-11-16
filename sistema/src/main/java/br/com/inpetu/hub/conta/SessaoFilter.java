package br.com.inpetu.hub.conta;

import java.io.IOException;
import java.io.Serial;
import java.time.Duration;
import java.util.Optional;

import br.com.inpetu.hub.auditoria.Auditoria;
import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.Telas;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Confere a sessão a cada pedido (conta ativa, mesma versão, prazo absoluto de 2 h) e protege a Área Interna: sem
 * login, {@code /admin} e {@code /conta} levam ao login; {@code /admin/config} é só do ADMIN.
 */
final class SessaoFilter extends HttpFilter {

    static final String NA_SESSAO = "inpetu.conta";
    /** Atributo do pedido com a {@link ContaSessao} já conferida. */
    static final String NO_PEDIDO = "conta";
    static final long PRAZO_ABSOLUTO = Duration.ofHours(2).toMillis();

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Ambiente ambiente;
    private final transient ContaRepositorio contas;
    private final transient Auditoria auditoria;

    SessaoFilter(Ambiente ambiente) {
        this.ambiente = ambiente;
        this.contas = new ContaRepositorio(ambiente.banco());
        this.auditoria = new Auditoria(ambiente.banco(), ambiente.relogio());
    }

    @Override
    protected void doFilter(HttpServletRequest pedido, HttpServletResponse resposta, FilterChain cadeia)
            throws IOException, ServletException {
        String caminho = Telas.caminho(pedido);
        boolean protegido = caminho.equals("/admin") || caminho.startsWith("/admin/") || caminho.startsWith(
                "/conta/");
        HttpSession sessao = pedido.getSession(false);
        ContaSessao conta = sessao == null ? null : sessao.getAttribute(NA_SESSAO) instanceof ContaSessao c ? c : null;
        if (conta != null && !valida(conta)) {
            sessao.invalidate();
            auditoria.seguranca(conta.id(), "SESSAO_REVOGADA", "NEGADO");
            if (protegido) {
                Telas.redirecionar(resposta, "/login?expirou");
                return;
            }
            conta = null;
        }
        if (protegido && conta == null) {
            Telas.redirecionar(resposta, "/login");
            return;
        }
        if (conta != null && administracao(caminho) && conta.perfil() != Perfil.ADMIN) {
            auditoria.seguranca(conta.id(), "ACESSO_NEGADO", "NEGADO");
            resposta.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (conta != null) {
            pedido.setAttribute(NO_PEDIDO, conta);
        }
        cadeia.doFilter(pedido, resposta);
    }

    private boolean valida(ContaSessao conta) {
        if (ambiente.relogio().millis() - conta.iniciadaEm() > PRAZO_ABSOLUTO) {
            return false;
        }
        Optional<Conta> atual = contas.porId(conta.id());
        return atual.isPresent() && conta.confere(atual.get());
    }

    private static boolean administracao(String caminho) {
        return caminho.equals("/admin/config") || caminho.startsWith("/admin/config/");
    }
}

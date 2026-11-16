package br.com.inpetu.hub.espaco;

import java.io.IOException;
import java.io.Serial;
import java.util.Optional;

import br.com.inpetu.hub.auditoria.Auditoria;
import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.Telas;
import br.com.inpetu.hub.comum.log.Eventos;
import br.com.inpetu.hub.conta.ContaSessao;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Cadastro de espaços da Área Interna (os dois perfis): lista, formulário, salvar e excluir. Formulário inválido volta
 * preenchido com o que foi digitado, sem gravar nada.
 */
public final class EspacosServlet extends HttpServlet {

    public static final String RAIZ = "/admin/cadastros/espacos";
    private static final String ALVO = "espacos";

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient EspacoRepositorio espacos;
    private final transient Auditoria auditoria;

    public EspacosServlet(Ambiente ambiente) {
        this.espacos = new EspacoRepositorio(ambiente.banco());
        this.auditoria = new Auditoria(ambiente.banco(), ambiente.relogio());
    }

    @Override
    protected void doGet(HttpServletRequest pedido, HttpServletResponse resposta) throws ServletException,
            IOException {
        switch (Telas.caminho(pedido)) {
            case RAIZ -> lista(pedido, resposta);
            case RAIZ + "/editar" -> editar(pedido, resposta);
            default -> resposta.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest pedido, HttpServletResponse resposta) throws ServletException,
            IOException {
        switch (Telas.caminho(pedido)) {
            case RAIZ + "/salvar" -> salvar(pedido, resposta);
            case RAIZ + "/excluir" -> excluir(pedido, resposta);
            default -> resposta.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void lista(HttpServletRequest pedido, HttpServletResponse resposta) throws ServletException,
            IOException {
        pedido.setAttribute("espacos", espacos.todos());
        if (pedido.getParameter("salvo") != null) {
            pedido.setAttribute("aviso", "Espaço salvo.");
        } else if (pedido.getParameter("excluido") != null) {
            pedido.setAttribute("aviso", "Espaço excluído.");
        }
        Telas.mostrar(pedido, resposta, "espaco/lista");
    }

    private void editar(HttpServletRequest pedido, HttpServletResponse resposta) throws ServletException,
            IOException {
        String id = pedido.getParameter("id");
        if (id == null) {
            formulario(pedido, resposta, EspacoFormulario.novo());
            return;
        }
        Optional<Espaco> espaco = numero(id).flatMap(espacos::porId);
        if (espaco.isEmpty()) {
            resposta.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        formulario(pedido, resposta, EspacoFormulario.de(espaco.get()));
    }

    private void salvar(HttpServletRequest pedido, HttpServletResponse resposta) throws ServletException,
            IOException {
        EspacoFormulario formulario = EspacoFormulario.de(pedido);
        Optional<Long> id = formulario.id.isEmpty() ? Optional.empty() : numero(formulario.id);
        if (!formulario.id.isEmpty() && (id.isEmpty() || espacos.porId(id.get()).isEmpty())) {
            resposta.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        EspacoFormulario.Dados dados = formulario.conferir();
        Long autor = autor(pedido);
        if (dados == null) {
            Eventos.auditoria("CADASTRO_INVALIDO", ALVO, "NEGADO");
            resposta.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            formulario(pedido, resposta, formulario);
            return;
        }
        if (id.isEmpty()) {
            long novo = espacos.inserir(dados);
            auditoria.registrar(autor, "CADASTRO_CRIADO", ALVO, novo, "OK", null);
        } else {
            espacos.atualizar(id.get(), dados);
            auditoria.registrar(autor, "CADASTRO_ALTERADO", ALVO, id.get(), "OK", null);
        }
        Telas.redirecionar(resposta, RAIZ + "?salvo");
    }

    private void excluir(HttpServletRequest pedido, HttpServletResponse resposta) throws IOException {
        Optional<Long> id = numero(pedido.getParameter("id"));
        if (id.isEmpty() || !espacos.excluir(id.get())) {
            resposta.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        auditoria.registrar(autor(pedido), "CADASTRO_EXCLUIDO", ALVO, id.get(), "OK", null);
        Telas.redirecionar(resposta, RAIZ + "?excluido");
    }

    private static void formulario(HttpServletRequest pedido, HttpServletResponse resposta,
            EspacoFormulario formulario) throws ServletException, IOException {
        pedido.setAttribute("formulario", formulario);
        Telas.mostrar(pedido, resposta, "espaco/formulario");
    }

    private static Long autor(HttpServletRequest pedido) {
        ContaSessao conta = ContaSessao.doPedido(pedido);
        return conta == null ? null : conta.id();
    }

    private static Optional<Long> numero(String valor) {
        return valor != null && valor.matches("[0-9]{1,18}") ? Optional.of(Long.parseLong(valor)) : Optional.empty();
    }
}

package br.com.inpetu.hub.solicitacao;

import java.io.IOException;
import java.io.Serial;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.RegraException;
import br.com.inpetu.hub.comum.Telas;
import br.com.inpetu.hub.comum.log.Eventos;
import br.com.inpetu.hub.empreendedor.Documentos;
import br.com.inpetu.hub.espaco.Espaco;
import br.com.inpetu.hub.espaco.EspacoRepositorio;
import br.com.inpetu.hub.reserva.DadosDoBanco;
import br.com.inpetu.hub.reserva.Horarios;
import br.com.inpetu.hub.reserva.Orcamento;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * O pedido público: {@code GET /reserva} (formulário), {@code POST /reservas/resumo} (confere e mostra o orçamento),
 * {@code POST /reservas/solicitar} (grava o pedido) e {@code GET /reservas/recebida}.
 */
final class PedidoServlet extends HttpServlet {

    static final String RASCUNHO = "pedido.rascunho";
    private static final String CONFERIDO = "pedido.conferido";
    private static final String CHAVE = "pedido.chave";
    private static final String REFERENCIA = "pedido.referencia";
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Serial
    private static final long serialVersionUID = 1L;

    /** Uma opção de lista na tela. */
    public record Opcao(String valor, String rotulo, boolean selecionado) {
    }

    private final transient Ambiente ambiente;
    private final transient SolicitacaoServico servico;
    private final transient EspacoRepositorio espacos;
    private final transient DadosDoBanco dados;

    PedidoServlet(Ambiente ambiente) {
        this.ambiente = ambiente;
        this.servico = new SolicitacaoServico(ambiente);
        this.espacos = new EspacoRepositorio(ambiente.banco());
        this.dados = new DadosDoBanco(ambiente.banco());
    }

    @Override
    protected void doGet(HttpServletRequest pedido, HttpServletResponse resposta) throws ServletException,
            IOException {
        switch (Telas.caminho(pedido)) {
            case "/reserva" -> formularioNovo(pedido, resposta);
            case "/reservas/recebida" -> recebida(pedido, resposta);
            default -> resposta.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest pedido, HttpServletResponse resposta) throws ServletException,
            IOException {
        switch (Telas.caminho(pedido)) {
            case "/reservas/resumo" -> resumo(pedido, resposta);
            case "/reservas/solicitar" -> solicitar(pedido, resposta);
            default -> resposta.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void formularioNovo(HttpServletRequest pedido, HttpServletResponse resposta) throws ServletException,
            IOException {
        HttpSession sessao = pedido.getSession(false);
        if (pedido.getParameter("editar") != null && sessao != null && sessao.getAttribute(
                RASCUNHO) instanceof PedidoFormulario rascunho) {
            formulario(pedido, resposta, rascunho);
            return;
        }
        // Só os campos que a agenda manda; o resto começa em branco.
        Map<String, String> daAgenda = Map.of("data", texto(pedido, "data"), "espacoId", texto(pedido, "espacoId"),
                "horaInicio", texto(pedido, "horaInicio"), "horaFim", texto(pedido, "horaFim"));
        formulario(pedido, resposta, PedidoFormulario.de(daAgenda::get));
    }

    private void resumo(HttpServletRequest pedido, HttpServletResponse resposta) throws ServletException,
            IOException {
        PedidoFormulario formulario = PedidoFormulario.de(pedido::getParameter);
        PedidoConferido conferido = formulario.conferir();
        if (conferido == null) {
            Eventos.auditoria("PEDIDO_INVALIDO", SolicitacaoServico.ALVO, "NEGADO");
            resposta.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            formulario(pedido, resposta, formulario);
            return;
        }
        Estimativa estimativa;
        try {
            estimativa = servico.estimar(conferido);
        } catch (RegraException regra) {
            Eventos.auditoria("PEDIDO_RECUSADO", SolicitacaoServico.ALVO, "NEGADO");
            resposta.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            pedido.setAttribute("erroGeral", regra.getMessage());
            formulario(pedido, resposta, formulario);
            return;
        }
        HttpSession sessao = pedido.getSession(true);
        sessao.setAttribute(RASCUNHO, formulario);
        sessao.setAttribute(CONFERIDO, conferido);
        sessao.setAttribute(CHAVE, UUID.randomUUID().toString());
        pedido.setAttribute("linhas", linhasDoResumo(conferido, estimativa));
        pedido.setAttribute("valores", valoresDoResumo(estimativa));
        pedido.setAttribute("taxasConfiguradas", estimativa.orcamento().taxasConfiguradas());
        Telas.mostrar(pedido, resposta, "solicitacao/resumo");
    }

    private void solicitar(HttpServletRequest pedido, HttpServletResponse resposta) {
        HttpSession sessao = pedido.getSession(false);
        if (sessao == null || !(sessao.getAttribute(CONFERIDO) instanceof PedidoConferido conferido)) {
            throw new RegraException("Preencha o formulário antes de confirmar.");
        }
        String referencia = servico.registrar(conferido, (String) sessao.getAttribute(CHAVE));
        sessao.removeAttribute(CONFERIDO);
        sessao.removeAttribute(RASCUNHO);
        sessao.removeAttribute(CHAVE);
        sessao.setAttribute(REFERENCIA, referencia);
        Telas.redirecionar(resposta, "/reservas/recebida");
    }

    private void recebida(HttpServletRequest pedido, HttpServletResponse resposta) throws ServletException,
            IOException {
        HttpSession sessao = pedido.getSession(false);
        pedido.setAttribute("enviado", sessao != null && sessao.getAttribute(REFERENCIA) != null);
        Telas.mostrar(pedido, resposta, "solicitacao/recebida");
    }

    private void formulario(HttpServletRequest pedido, HttpServletResponse resposta, PedidoFormulario formulario)
            throws ServletException, IOException {
        LocalDate hoje = LocalDate.now(ambiente.relogio());
        pedido.setAttribute("f", formulario);
        pedido.setAttribute("v", formulario.getValores());
        pedido.setAttribute("erros", formulario.getErros());
        pedido.setAttribute("rotulos", PedidoFormulario.ROTULOS);
        pedido.setAttribute("hoje", hoje.toString());
        pedido.setAttribute("ultimoDia", Horarios.ultimoDia(ambiente.relogio()).toString());
        List<Opcao> listaEspacos = new ArrayList<>();
        for (Espaco e : espacos.reservaveis()) {
            listaEspacos.add(new Opcao(String.valueOf(e.id()), e.nome() + " · " + e.descricao(), String.valueOf(e.id())
                    .equals(formulario.valor("espacoId"))));
        }
        List<Opcao> personas = new ArrayList<>();
        for (DadosDoBanco.PersonaComNome p : dados.personas()) {
            personas.add(new Opcao(String.valueOf(p.persona().id()), p.persona().codigo() + " — " + p.nome(), String
                    .valueOf(p.persona().id()).equals(formulario.valor("personaId"))));
        }
        List<Opcao> tipos = new ArrayList<>();
        Documentos.TIPOS.forEach((valor, rotulo) -> tipos.add(new Opcao(valor, rotulo, valor.equals(formulario.valor(
                "tipoDocumento")))));
        pedido.setAttribute("espacos", listaEspacos);
        pedido.setAttribute("personas", personas);
        pedido.setAttribute("tiposDocumento", tipos);
        Telas.mostrar(pedido, resposta, "solicitacao/formulario");
    }

    private static Map<String, String> linhasDoResumo(PedidoConferido p, Estimativa e) {
        Orcamento o = e.orcamento();
        Map<String, String> linhas = new LinkedHashMap<>();
        linhas.put("Solicitante", p.nome());
        linhas.put("Documento", Documentos.exibir(p.tipoDocumento(), p.documento()));
        linhas.put("Empresa", p.empresa() == null ? "Não informada" : p.empresa());
        linhas.put("Email", p.email());
        linhas.put("Telefone", p.telefone());
        linhas.put("Cidade", p.cidade());
        linhas.put("Evento", p.titulo());
        linhas.put("Objetivo", p.objetivo());
        linhas.put("Espaço", e.espaco().nome());
        linhas.put("Data e horário", DIA.format(p.data()) + " · " + p.inicio() + "–" + p.fim());
        linhas.put("Duração", horas(o.horas()) + " horas / " + o.turnos() + " turno(s)");
        linhas.put("Persona", e.persona().persona().codigo() + " — " + e.persona().nome());
        linhas.put("Participantes", String.valueOf(p.participantes()));
        return linhas;
    }

    private static Map<String, String> valoresDoResumo(Estimativa e) {
        Orcamento o = e.orcamento();
        Map<String, String> valores = new LinkedHashMap<>();
        valores.put("Usa a cota mensal (15 dias de antecedência)", o.usaCota() ? "Sim" : "Não");
        valores.put("Cota do mês: total / usada / saldo", horas(e.cota().horasTotais()) + " h / " + horas(e.cota()
                .utilizadas()) + " h / " + horas(e.cota().saldo()) + " h");
        valores.put("Locação / operação / limpeza", o.locacao() + "% / " + o.operacao() + "% / " + o.limpeza() + "%");
        valores.put("Locação de referência", dinheiro(o.valorBase()));
        valores.put("Operação / limpeza", dinheiro(o.valorOperacao()) + " / " + dinheiro(o.valorLimpeza()));
        valores.put("Valor estimado", dinheiro(o.valorFinal()));
        valores.put("Sinal", o.percentualSinal() + "% · " + dinheiro(o.valorSinal()));
        valores.put("Restante", dinheiro(o.valorRestante()));
        return valores;
    }

    private static String dinheiro(BigDecimal valor) {
        return "R$ " + new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR")))
                .format(valor);
    }

    private static String horas(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static String texto(HttpServletRequest pedido, String nome) {
        String valor = pedido.getParameter(nome);
        return valor == null ? "" : valor;
    }
}

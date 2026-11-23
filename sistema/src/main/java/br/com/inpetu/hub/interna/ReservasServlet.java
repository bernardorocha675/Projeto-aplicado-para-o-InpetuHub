package br.com.inpetu.hub.interna;

import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.Formatos;
import br.com.inpetu.hub.comum.RegraException;
import br.com.inpetu.hub.comum.Telas;
import br.com.inpetu.hub.conta.ContaSessao;
import br.com.inpetu.hub.conta.Perfil;
import br.com.inpetu.hub.espaco.Espaco;
import br.com.inpetu.hub.espaco.EspacoRepositorio;
import br.com.inpetu.hub.reserva.DadosDoBanco;
import br.com.inpetu.hub.reserva.ReservaConsultas;
import br.com.inpetu.hub.reserva.ReservaServico;
import br.com.inpetu.hub.reserva.SituacaoPagamento;
import br.com.inpetu.hub.reserva.SituacaoReserva;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Reservas: lista com filtros ({@code /admin/reservas}), detalhe ({@code /{id}}) e as ações ({@code /{id}/acao}). */
final class ReservasServlet extends HttpServlet {

    static final String RAIZ = "/admin/reservas";
    private static final Map<String, String> AVISOS = Map.of("aceita",
            "Solicitação encaminhada. Confira a persona e os dados e aprove ou recuse a reserva.", "aprovada",
            "Reserva aprovada.", "recusada", "Reserva recusada e horário liberado.", "alterada",
            "Reserva alterada e valores recalculados.", "cancelada",
            "Reserva cancelada. Consulte o histórico para a movimentação de cota.", "pagamento",
            "Registro de pagamento atualizado.", "concluida", "Reserva concluída.");

    @Serial
    private static final long serialVersionUID = 1L;

    /** Uma opção de lista. */
    public record Opcao(String valor, String rotulo, boolean selecionado) {
    }

    private final transient Ambiente ambiente;
    private final transient ReservaConsultas consultas;
    private final transient ReservaServico servico;
    private final transient EspacoRepositorio espacos;
    private final transient DadosDoBanco dados;

    ReservasServlet(Ambiente ambiente) {
        this.ambiente = ambiente;
        this.consultas = new ReservaConsultas(ambiente.banco());
        this.servico = new ReservaServico(ambiente);
        this.espacos = new EspacoRepositorio(ambiente.banco());
        this.dados = new DadosDoBanco(ambiente.banco());
    }

    @Override
    protected void doGet(HttpServletRequest pedido, HttpServletResponse resposta) throws ServletException,
            IOException {
        String caminho = Telas.caminho(pedido);
        if (caminho.equals(RAIZ)) {
            lista(pedido, resposta);
            return;
        }
        String resto = caminho.substring(RAIZ.length() + 1);
        if (!resto.matches("[0-9]{1,18}")) {
            resposta.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Optional<ReservaConsultas.Detalhe> detalhe = consultas.detalhe(Long.parseLong(resto));
        if (detalhe.isEmpty()) {
            resposta.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        detalhe(pedido, resposta, detalhe.get());
    }

    @Override
    protected void doPost(HttpServletRequest pedido, HttpServletResponse resposta) throws IOException {
        String[] partes = Telas.caminho(pedido).substring(RAIZ.length()).split("/");
        if (partes.length != 3 || !partes[1].matches("[0-9]{1,18}")) {
            resposta.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        long id = Long.parseLong(partes[1]);
        ContaSessao conta = ContaSessao.doPedido(pedido);
        long colaborador = conta.id();
        String ok = switch (partes[2]) {
            case "aprovar" -> {
                servico.aprovar(id, colaborador);
                yield "aprovada";
            }
            case "recusar" -> {
                servico.recusar(id, pedido.getParameter("motivo"), colaborador);
                yield "recusada";
            }
            case "cancelar" -> {
                servico.cancelar(id, "sim".equals(pedido.getParameter("forcaMaior")), pedido.getParameter(
                        "justificativa"), colaborador, conta.perfil() == Perfil.ADMIN);
                yield "cancelada";
            }
            case "alterar" -> {
                servico.alterar(id, Numeros.data(pedido.getParameter("data")), Numeros.hora(pedido.getParameter(
                        "horaInicio")), Numeros.hora(pedido.getParameter("horaFim")), Numeros.inteiro(
                                pedido
                                        .getParameter("espacoId")),
                        Numeros.inteiro(pedido.getParameter("personaId")),
                        colaborador);
                yield "alterada";
            }
            case "pagamento" -> {
                servico.registrarPagamento(id, pagamento(pedido.getParameter("situacao")), colaborador);
                yield "pagamento";
            }
            case "concluir" -> {
                servico.concluir(id, colaborador);
                yield "concluida";
            }
            default -> null;
        };
        if (ok == null) {
            resposta.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Telas.redirecionar(resposta, RAIZ + "/" + id + "?ok=" + ok);
    }

    private void lista(HttpServletRequest pedido, HttpServletResponse resposta) throws ServletException,
            IOException {
        String situacao = texto(pedido, "status");
        String data = texto(pedido, "data");
        String espaco = texto(pedido, "espaco");
        String persona = texto(pedido, "persona");
        ReservaConsultas.Filtro filtro = new ReservaConsultas.Filtro(situacao.isEmpty() ? null : situacao(situacao),
                data.isEmpty() ? null : Numeros.data(data), espaco.isEmpty() ? null : Numeros.inteiro(espaco), persona
                        .isEmpty() ? null : Numeros.inteiro(persona),
                texto(pedido, "busca"));
        MenuInterno.preparar(pedido, ambiente, "reservas");
        pedido.setAttribute("linhas", Linhas.de(consultas.listar(filtro)));
        List<Opcao> situacoes = new ArrayList<>();
        for (SituacaoReserva s : SituacaoReserva.values()) {
            situacoes.add(new Opcao(s.name(), s.rotulo(), s.name().equals(situacao)));
        }
        pedido.setAttribute("situacoes", situacoes);
        pedido.setAttribute("espacos", opcoesDeEspaco(espaco));
        pedido.setAttribute("personas", opcoesDePersona(persona));
        pedido.setAttribute("filtroData", data);
        pedido.setAttribute("busca", texto(pedido, "busca"));
        Telas.mostrar(pedido, resposta, "interna/reservas");
    }

    private void detalhe(HttpServletRequest pedido, HttpServletResponse resposta, ReservaConsultas.Detalhe d)
            throws ServletException, IOException {
        MenuInterno.preparar(pedido, ambiente, "reservas");
        ContaSessao conta = ContaSessao.doPedido(pedido);
        ReservaConsultas.Linha l = d.linha();
        Map<String, String> solicitante = new LinkedHashMap<>();
        solicitante.put("Solicitante", l.solicitante());
        solicitante.put("Empresa / documento", (l.empresa() == null ? "Não informada" : l.empresa()) + " / " + l
                .documento());
        solicitante.put("Email", d.email());
        solicitante.put("Telefone / cidade", d.telefone() + " / " + Formatos.ouTraco(d.cidade()));
        solicitante.put("Segmento / cargo", Formatos.ouTraco(d.segmento()) + " / " + Formatos.ouTraco(d.cargo()));
        solicitante.put("Objetivo", d.objetivo());
        solicitante.put("Observações", Formatos.ouTraco(d.observacoes()));
        solicitante.put("Participantes", String.valueOf(d.participantes()));
        solicitante.put("Data e horário", Formatos.dia(l.data()) + " · " + l.inicio() + "–" + l.fim());
        solicitante.put("Espaço", l.espaco());
        solicitante.put("Persona declarada", l.persona() + " — " + d.personaNome());
        solicitante.put("Pedido em", Formatos.diaHora(d.solicitadaEm()));
        solicitante.put("Inscrição paga / financiado / acadêmico", Formatos.simNao(d.inscricaoPaga()) + " / "
                + Formatos.simNao(d.financiado()) + " / " + Formatos.simNao(d.academico()));
        Map<String, String> valores = new LinkedHashMap<>();
        valores.put("Usa a cota mensal", Formatos.simNao(d.usaCota()) + (d.usaCota()
                ? " (" + Formatos.horas(d
                        .horasCota()) + " h)"
                : ""));
        valores.put("Cota do mês: total / usada / saldo", Formatos.horas(d.cota().horasTotais()) + " h / " + Formatos
                .horas(d.cota().utilizadas()) + " h / " + Formatos.horas(d.cota().saldo()) + " h");
        valores.put("Dívida de cota que passa adiante", Formatos.horas(d.cota().saldoDevedor()) + " h");
        valores.put("Locação / operação / limpeza", d.locacao() + "% / " + d.operacao() + "% / " + d.limpeza() + "%");
        valores.put("Locação de referência", Formatos.dinheiro(d.valorBase()));
        valores.put("Operação / limpeza", Formatos.dinheiro(d.valorOperacao()) + " / " + Formatos.dinheiro(d
                .valorLimpeza()));
        valores.put("Valor final", Formatos.dinheiro(d.valorFinal()));
        valores.put("Sinal", d.percentualSinal() + "% · " + Formatos.dinheiro(d.valorSinal()));
        valores.put("Restante", Formatos.dinheiro(d.valorRestante()));
        valores.put("Pagamento", d.pagamento().rotulo());
        valores.put("Responsável", d.responsavel() == null ? "Ainda não atribuído" : d.responsavel());
        List<String[]> historico = d.historico().stream()
                .map(h -> new String[]{h.acao(), Formatos.diaHora(h.quando()) + " · " + h.quem(), Formatos.ouTraco(h
                        .descricao())})
                .toList();
        SituacaoReserva s = l.situacao();
        pedido.setAttribute("id", l.id());
        pedido.setAttribute("titulo", d.titulo());
        pedido.setAttribute("situacao", s.rotulo());
        pedido.setAttribute("classeSituacao", s.name());
        pedido.setAttribute("solicitante", solicitante);
        pedido.setAttribute("valores", valores);
        pedido.setAttribute("historico", historico);
        pedido.setAttribute("taxasConfiguradas", d.taxasConfiguradas());
        pedido.setAttribute("motivoRecusa", d.motivoRecusa());
        pedido.setAttribute("podeAnalisar", s == SituacaoReserva.PENDENTE);
        pedido.setAttribute("podeAlterar", s == SituacaoReserva.PENDENTE || s == SituacaoReserva.APROVADA);
        pedido.setAttribute("podePagar", s == SituacaoReserva.APROVADA || s == SituacaoReserva.CONCLUIDA);
        pedido.setAttribute("podeConcluir", s == SituacaoReserva.APROVADA);
        pedido.setAttribute("gestor", conta.perfil() == Perfil.ADMIN);
        pedido.setAttribute("dataValor", l.data().toString());
        pedido.setAttribute("inicioValor", l.inicio().toString());
        pedido.setAttribute("fimValor", l.fim().toString());
        pedido.setAttribute("espacos", opcoesDeEspaco(String.valueOf(d.espacoId())));
        pedido.setAttribute("personas", opcoesDePersona(String.valueOf(d.personaId())));
        List<Opcao> pagamentos = new ArrayList<>();
        for (SituacaoPagamento p : SituacaoPagamento.values()) {
            pagamentos.add(new Opcao(p.name(), p.rotulo(), p == d.pagamento()));
        }
        pedido.setAttribute("pagamentos", pagamentos);
        String ok = pedido.getParameter("ok");
        pedido.setAttribute("aviso", ok == null ? null : AVISOS.get(ok));
        Telas.mostrar(pedido, resposta, "interna/reserva");
    }

    private List<Opcao> opcoesDeEspaco(String selecionado) {
        List<Opcao> opcoes = new ArrayList<>();
        for (Espaco e : espacos.todos()) {
            opcoes.add(new Opcao(String.valueOf(e.id()), e.nome() + (e.reservavel() ? "" : " (fora da agenda)"), String
                    .valueOf(e.id()).equals(selecionado)));
        }
        return opcoes;
    }

    private List<Opcao> opcoesDePersona(String selecionado) {
        List<Opcao> opcoes = new ArrayList<>();
        for (DadosDoBanco.PersonaComNome p : dados.personas()) {
            opcoes.add(new Opcao(String.valueOf(p.persona().id()), p.persona().codigo() + " — " + p.nome(), String
                    .valueOf(p.persona().id()).equals(selecionado)));
        }
        return opcoes;
    }

    private static SituacaoReserva situacao(String valor) {
        try {
            return SituacaoReserva.valueOf(valor);
        } catch (IllegalArgumentException desconhecida) {
            throw new RegraException("Escolha uma situação da lista.");
        }
    }

    private static SituacaoPagamento pagamento(String valor) {
        try {
            return SituacaoPagamento.valueOf(valor == null ? "" : valor);
        } catch (IllegalArgumentException desconhecida) {
            throw new RegraException("Escolha uma situação de pagamento da lista.");
        }
    }

    private static String texto(HttpServletRequest pedido, String nome) {
        String valor = pedido.getParameter(nome);
        return valor == null ? "" : valor.strip();
    }
}

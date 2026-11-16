package br.com.inpetu.hub.solicitacao;

import java.io.Serial;
import java.io.Serializable;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import br.com.inpetu.hub.comum.RegraException;
import br.com.inpetu.hub.empreendedor.Contatos;
import br.com.inpetu.hub.empreendedor.Documentos;

/**
 * O formulário do pedido público como foi digitado (para voltar à tela igual) e a conferência dele, com a primeira
 * mensagem de cada campo na ordem da tela. Caixa desmarcada não vem no envio: vale Não.
 */
public final class PedidoFormulario implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Rótulos na ordem da tela (o resumo de erros usa “Rótulo: mensagem”). */
    public static final Map<String, String> ROTULOS = rotulos();

    private static final String PREENCHA = "Preencha este campo.";
    private static final String CONVERSAO = "Confira a data, o horário ou o número informado.";

    private final LinkedHashMap<String, String> valores = new LinkedHashMap<>();
    private final boolean inscricaoPaga;
    private final boolean financiado;
    private final boolean academico;
    private final LinkedHashMap<String, String> erros = new LinkedHashMap<>();

    private PedidoFormulario(Function<String, String> campo) {
        for (String nome : ROTULOS.keySet()) {
            String valor = campo.apply(nome);
            valores.put(nome, valor == null ? "" : valor);
        }
        if (valores.get("tipoDocumento").isEmpty()) {
            valores.put("tipoDocumento", Documentos.CNPJ);
        }
        if (valores.get("numeroParticipantes").isEmpty() && campo.apply("numeroParticipantes") == null) {
            valores.put("numeroParticipantes", "1");
        }
        inscricaoPaga = campo.apply("inscricaoPaga") != null;
        financiado = campo.apply("financiado") != null;
        academico = campo.apply("academico") != null;
    }

    private static Map<String, String> rotulos() {
        Map<String, String> r = new LinkedHashMap<>();
        r.put("data", "Data");
        r.put("espacoId", "Espaço");
        r.put("horaInicio", "Hora inicial");
        r.put("horaFim", "Hora final");
        r.put("nome", "Nome");
        r.put("tipoDocumento", "Documento");
        r.put("documento", "Número do documento");
        r.put("empresa", "Empresa/instituição");
        r.put("email", "Email");
        r.put("telefone", "Telefone");
        r.put("cidade", "Cidade");
        r.put("segmento", "Segmento");
        r.put("cargo", "Cargo");
        r.put("titulo", "Nome do evento");
        r.put("personaId", "Persona declarada");
        r.put("numeroParticipantes", "Quantidade de participantes");
        r.put("objetivo", "Objetivo");
        r.put("observacoes", "Observações");
        return Collections.unmodifiableMap(r);
    }

    /** {@code campo} devolve o valor enviado ou null quando o campo não veio. */
    public static PedidoFormulario de(Function<String, String> campo) {
        return new PedidoFormulario(campo);
    }

    /** Confere os campos; devolve o pedido conferido ou null, com os erros preenchidos. */
    public PedidoConferido conferir() {
        erros.clear();
        LocalDate data = conversao("data", LocalDate::parse);
        Long espacoId = conversao("espacoId", Long::valueOf);
        LocalTime inicio = conversao("horaInicio", LocalTime::parse);
        LocalTime fim = conversao("horaFim", LocalTime::parse);
        String nome = texto("nome", 255, true);
        if (nome != null && !Contatos.nomeValido(nome)) {
            erros.put("nome", "Informe um nome com letras, sem números.");
        }
        Documentos.Documento documento = null;
        if (valores.get("documento").length() > 30) {
            erros.put("documento", "Use até 30 caracteres.");
        } else {
            try {
                documento = Documentos.normalizar(valores.get("tipoDocumento"), valores.get("documento"));
            } catch (RegraException e) {
                erros.put(e.getMessage().startsWith("Escolha") ? "tipoDocumento" : "documento", e.getMessage());
            }
        }
        String empresa = texto("empresa", 255, false);
        String email = texto("email", 255, true);
        if (email != null && !Contatos.emailValido(email)) {
            erros.put("email", "Informe um email completo, como nome@instituicao.com.br.");
        }
        String telefone = null;
        if (texto("telefone", 255, true) != null) {
            try {
                telefone = Contatos.telefone(valores.get("telefone"));
            } catch (RegraException e) {
                erros.put("telefone", e.getMessage());
            }
        }
        String cidade = texto("cidade", 255, true);
        String segmento = texto("segmento", 255, false);
        String cargo = texto("cargo", 255, false);
        String titulo = texto("titulo", 255, true);
        Long personaId = conversao("personaId", Long::valueOf);
        Integer participantes = participantes();
        String objetivo = texto("objetivo", 2000, true);
        String observacoes = texto("observacoes", 2000, false);
        if (!erros.isEmpty()) {
            ordenarErros();
            return null;
        }
        return new PedidoConferido(espacoId, personaId, data, inicio, fim, participantes, inscricaoPaga, financiado,
                academico, titulo, objetivo, observacoes, nome, documento.tipo(), documento.numero(), email, telefone,
                empresa, cidade, segmento, cargo);
    }

    private <T> T conversao(String nome, Function<String, T> conversor) {
        String valor = valores.get(nome).strip();
        if (valor.isEmpty()) {
            erros.put(nome, PREENCHA);
            return null;
        }
        try {
            return conversor.apply(valor);
        } catch (DateTimeException | NumberFormatException invalido) {
            erros.put(nome, CONVERSAO);
            return null;
        }
    }

    /** Texto sem os espaços das pontas; vazio vira null (ou erro, se obrigatório). */
    private String texto(String nome, int maximo, boolean obrigatorio) {
        String valor = valores.get(nome).strip();
        if (valor.isEmpty()) {
            if (obrigatorio) {
                erros.put(nome, PREENCHA);
            }
            return null;
        }
        if (valor.length() > maximo) {
            erros.put(nome, "Use até " + maximo + " caracteres.");
            return null;
        }
        return valor;
    }

    private Integer participantes() {
        String valor = valores.get("numeroParticipantes").strip();
        if (valor.isEmpty()) {
            erros.put("numeroParticipantes", PREENCHA);
            return null;
        }
        if (!valor.matches("[0-9]{1,9}") || Integer.parseInt(valor) < 1) {
            erros.put("numeroParticipantes", "Informe um número inteiro a partir de 1.");
            return null;
        }
        return Integer.valueOf(valor);
    }

    private void ordenarErros() {
        Map<String, String> ordenados = new LinkedHashMap<>();
        for (String nome : ROTULOS.keySet()) {
            if (erros.containsKey(nome)) {
                ordenados.put(nome, erros.get(nome));
            }
        }
        erros.clear();
        erros.putAll(ordenados);
    }

    /** O valor digitado de um campo (para a tela). */
    public String valor(String nome) {
        return valores.getOrDefault(nome, "");
    }

    public Map<String, String> getValores() {
        return Collections.unmodifiableMap(valores);
    }

    public Map<String, String> getErros() {
        return erros;
    }

    public String getNome() {
        return valores.get("nome");
    }

    public boolean isInscricaoPaga() {
        return inscricaoPaga;
    }

    public boolean isFinanciado() {
        return financiado;
    }

    public boolean isAcademico() {
        return academico;
    }
}

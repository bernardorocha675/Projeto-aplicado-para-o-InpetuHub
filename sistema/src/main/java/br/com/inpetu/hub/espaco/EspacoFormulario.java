package br.com.inpetu.hub.espaco;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

/**
 * O formulário de espaço como foi enviado (para reapresentar exatamente o que a pessoa digitou) e a conferência dele.
 * Caixa desmarcada não vem no envio: vale Não.
 */
public final class EspacoFormulario {

    static final int MAXIMO_TEXTO = 255;

    /** Os dados já conferidos, prontos para gravar. */
    public record Dados(String nome, int capacidade, String mobiliario, boolean disponivel, boolean ativo) {
    }

    final String id;
    final String nome;
    final String capacidade;
    final String mobiliario;
    final boolean disponivel;
    final boolean ativo;
    /** Erros por campo, na ordem do formulário. */
    final Map<String, String> erros = new LinkedHashMap<>();

    private EspacoFormulario(String id, String nome, String capacidade, String mobiliario, boolean disponivel,
            boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.capacidade = capacidade;
        this.mobiliario = mobiliario;
        this.disponivel = disponivel;
        this.ativo = ativo;
    }

    static EspacoFormulario novo() {
        return new EspacoFormulario("", "", "", "", true, true);
    }

    static EspacoFormulario de(Espaco espaco) {
        return new EspacoFormulario(String.valueOf(espaco.id()), espaco.nome(), String.valueOf(espaco.capacidade()),
                espaco.mobiliario() == null ? "" : espaco.mobiliario(), espaco.disponivel(), espaco.ativo());
    }

    static EspacoFormulario de(HttpServletRequest pedido) {
        return new EspacoFormulario(texto(pedido, "id"), texto(pedido, "nome"), texto(pedido, "capacidade"), texto(
                pedido, "mobiliario"), pedido.getParameter("disponivel") != null, pedido.getParameter("ativo") != null);
    }

    /** Confere os campos; devolve os dados se estiver tudo certo, ou null (com os erros preenchidos). */
    Dados conferir() {
        String nomeLimpo = nome.strip();
        if (nomeLimpo.isEmpty()) {
            erros.put("nome", "Nome do espaço: preencha este campo.");
        } else if (nomeLimpo.length() > MAXIMO_TEXTO) {
            erros.put("nome", "Nome do espaço: use até 255 caracteres.");
        }
        Integer numero = capacidade.matches("[0-9]{1,10}") ? inteiro(capacidade) : null;
        if (numero == null || numero < 1) {
            erros.put("capacidade", "Capacidade: informe um número inteiro de 1 a 2147483647.");
        }
        String mobiliarioLimpo = mobiliario.strip();
        if (mobiliarioLimpo.length() > MAXIMO_TEXTO) {
            erros.put("mobiliario", "Mobiliário: use até 255 caracteres.");
        }
        if (!erros.isEmpty()) {
            return null;
        }
        return new Dados(nomeLimpo, numero, mobiliarioLimpo.isEmpty() ? null : mobiliarioLimpo, disponivel, ativo);
    }

    private static Integer inteiro(String valor) {
        long numero = Long.parseLong(valor);
        return numero > Integer.MAX_VALUE ? null : (int) numero;
    }

    private static String texto(HttpServletRequest pedido, String campo) {
        String valor = pedido.getParameter(campo);
        return valor == null ? "" : valor;
    }

    // Leitura pela tela (JSP).
    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCapacidade() {
        return capacidade;
    }

    public String getMobiliario() {
        return mobiliario;
    }

    public boolean isDisponivel() {
        return disponivel;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Map<String, String> getErros() {
        return erros;
    }
}

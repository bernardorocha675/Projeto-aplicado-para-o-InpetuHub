package br.com.inpetu.hub.interna;

import java.util.Map;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.conta.Contas;
import br.com.inpetu.hub.solicitacao.PedidoConferido;
import br.com.inpetu.hub.solicitacao.PedidoFormulario;
import br.com.inpetu.hub.solicitacao.SolicitacaoServico;

/**
 * Apoio dos testes da Área Interna: pedidos gravados como o público grava, com o relógio em 12/11/2026, 10h. As taxas
 * ficam zeradas (no banco novo elas não estão configuradas e a aprovação de reservas cobradas é recusada).
 */
final class Cenario {

    private static int sequencia;

    private Cenario() {
    }

    static void taxasZeradas(Ambiente ambiente) throws Exception {
        Contas.executar(ambiente.banco(),
                "update configuracao set taxa_operacao = 0, taxa_limpeza = 0, insumos_limpeza = 0");
    }

    static long espaco(Ambiente ambiente, String nome) throws Exception {
        return Long.parseLong(Contas.texto(ambiente.banco(), "select id_espaco from espaco where nome_espaco = ?",
                nome));
    }

    static long persona(Ambiente ambiente, String codigo) throws Exception {
        return Long.parseLong(Contas.texto(ambiente.banco(), "select id from persona where codigo = ?", codigo));
    }

    /** Campos de um pedido de 24/11/2026, das 8h às 12h, P6, CNPJ 12345678000190. */
    static Map<String, String> campos(Ambiente ambiente) throws Exception {
        Map<String, String> c = new java.util.HashMap<>(Map.of("espacoId", String.valueOf(espaco(ambiente,
                "Sala Maker VIA")), "personaId", String.valueOf(persona(ambiente, "P6")), "data", "2026-11-24",
                "horaInicio", "08:00", "horaFim", "12:00", "nome", "Maria Souza", "tipoDocumento", "CNPJ", "documento",
                "12345678000190", "email", "maria@example.com", "telefone", "48999991234"));
        c.put("empresa", "Instituição");
        c.put("cidade", "Florianópolis");
        c.put("titulo", "Encontro de teste");
        c.put("objetivo", "Conectar");
        c.put("numeroParticipantes", "10");
        return c;
    }

    static PedidoConferido conferido(Map<String, String> campos) {
        PedidoFormulario f = PedidoFormulario.de(campos::get);
        PedidoConferido p = f.conferir();
        if (p == null) {
            throw new AssertionError(f.getErros().toString());
        }
        return p;
    }

    /** Grava o pedido como o público faz e devolve o número dele em solicitacao_publica. */
    static long pedido(Ambiente ambiente, Map<String, String> campos) throws Exception {
        String referencia = new SolicitacaoServico(ambiente).registrar(conferido(campos), "chave-de-teste-" + String
                .format("%010d", ++sequencia));
        return Long.parseLong(Contas.texto(ambiente.banco(), "select id from solicitacao_publica where referencia = ?",
                referencia));
    }
}

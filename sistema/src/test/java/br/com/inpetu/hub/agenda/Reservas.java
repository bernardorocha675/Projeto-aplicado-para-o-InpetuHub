package br.com.inpetu.hub.agenda;

import javax.sql.DataSource;

import br.com.inpetu.hub.conta.Contas;

/** Apoio dos testes: grava reservas direto no banco (as regras de reserva chegam na entrega 6). */
final class Reservas {

    private Reservas() {
    }

    static long espaco(DataSource banco, String nome) throws Exception {
        return Long.parseLong(Contas.texto(banco, "select id_espaco from espaco where nome_espaco = ?", nome));
    }

    static void gravar(DataSource banco, long espaco, String data, String inicio, String fim, String situacao,
            String titulo) throws Exception {
        Contas.executar(banco, "insert into empreendedor(nome, email, ativo) values ('Pessoa', 'p@x.com', true)");
        Contas.executar(banco, "insert into reserva(id_empreendedor, id_espaco, persona_id, data, hora_inicio,"
                + " hora_fim, status_reserva, titulo, numero_participantes, inscricao_paga, financiado, academico,"
                + " usa_cota, percentual_sinal, percentual_locacao, percentual_operacao, percentual_limpeza,"
                + " taxas_configuradas) select max(id_empreendedor), ?, 1, ?, ?, ?, ?, ?, 10, false, false, false,"
                + " false, 0, 100, 0, 0, false from empreendedor", espaco, data, inicio, fim, situacao, titulo);
    }
}

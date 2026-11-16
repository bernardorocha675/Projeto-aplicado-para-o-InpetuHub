package br.com.inpetu.hub.interna;

import java.util.List;

import br.com.inpetu.hub.comum.Formatos;
import br.com.inpetu.hub.reserva.ReservaConsultas;

/** As linhas da tabela de reservas já com os textos da tela. */
public final class Linhas {

    /** Uma linha pronta para a tela. */
    public record Linha(long id, String dia, String horario, String solicitante, String empresa, String espaco,
            String persona, String situacao, String classe) {
    }

    private Linhas() {
    }

    public static List<Linha> de(List<ReservaConsultas.Linha> linhas) {
        return linhas.stream()
                .map(l -> new Linha(l.id(), Formatos.dia(l.data()), l.inicio() + "–" + l.fim(), l.solicitante(),
                        Formatos.ouTraco(l.empresa()), l.espaco(), l.persona(), l.situacao().rotulo(), l.situacao()
                                .name()))
                .toList();
    }
}

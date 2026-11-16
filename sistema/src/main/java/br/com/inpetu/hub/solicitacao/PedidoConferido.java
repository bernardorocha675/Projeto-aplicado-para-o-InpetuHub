package br.com.inpetu.hub.solicitacao;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

/** O pedido público já conferido: o que se pede e quem pede (documento e telefone normalizados). */
public record PedidoConferido(long espacoId, long personaId, LocalDate data, LocalTime inicio, LocalTime fim,
        int participantes, boolean inscricaoPaga, boolean financiado, boolean academico, String titulo, String objetivo,
        String observacoes, String nome, String tipoDocumento, String documento, String email, String telefone,
        String empresa, String cidade, String segmento, String cargo) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}

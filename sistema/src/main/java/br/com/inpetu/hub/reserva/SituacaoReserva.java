package br.com.inpetu.hub.reserva;

/** Situação da reserva, gravada pelo nome (os mesmos nomes do antigo). Só APROVADA ocupa horário e cota. */
public enum SituacaoReserva {

    PENDENTE("Pendente"), APROVADA("Aprovada"), RECUSADA("Recusada"), CANCELADA("Cancelada"), CONCLUIDA("Concluída");

    private final String rotulo;

    SituacaoReserva(String rotulo) {
        this.rotulo = rotulo;
    }

    /** O nome que aparece nas telas. */
    public String rotulo() {
        return rotulo;
    }
}

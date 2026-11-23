package br.com.inpetu.hub.reserva;

/** Registro manual do pagamento (o sistema não recebe pagamentos). */
public enum SituacaoPagamento {

    PENDENTE("Pendente"), SINAL_PAGO("Sinal pago"), PAGO("Pago");

    private final String rotulo;

    SituacaoPagamento(String rotulo) {
        this.rotulo = rotulo;
    }

    /** O nome que aparece nas telas. */
    public String rotulo() {
        return rotulo;
    }
}

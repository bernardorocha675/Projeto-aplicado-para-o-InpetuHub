package br.com.inpetu.hub.reserva;

/** Percentuais de locação, operação e limpeza por persona, como no antigo. */
public final class Taxacao {

    /** Percentuais (0 a 100) e se a limpeza usa a taxa de insumos. */
    public record Taxas(int locacao, int operacao, int limpeza, boolean insumos) {
    }

    private Taxacao() {
    }

    public static Taxas calcular(String codigo, boolean cota, boolean pago, boolean academico) {
        if (codigo.equals("P1")) {
            return new Taxas(0, 0, 0, false);
        }
        if (!cota) {
            return new Taxas(100, 100, 100, false);
        }
        if (codigo.equals("P2")) {
            if (pago) {
                return new Taxas(50, 100, 100, false);
            }
            if (academico) {
                return new Taxas(0, 0, 100, true);
            }
            return new Taxas(100, 100, 100, false);
        }
        return new Taxas(pago ? 75 : 0, 100, 100, false);
    }
}

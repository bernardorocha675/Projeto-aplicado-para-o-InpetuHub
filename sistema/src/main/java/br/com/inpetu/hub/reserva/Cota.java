package br.com.inpetu.hub.reserva;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Refaz a cota de um mês a partir dos movimentos da persona, como o antigo: as horas não acumulam, mas o que passou da
 * cota (penalidade) vira dívida e é descontado no mês seguinte.
 */
public final class Cota {

    private Cota() {
    }

    /** {@code movimentos} são só os da persona. */
    public static CotaDoMes doMes(Persona persona, List<Movimento> movimentos, YearMonth alvo) {
        BigDecimal totais = persona.cotaMensalHoras() == null ? BigDecimal.ZERO : persona.cotaMensalHoras();
        YearMonth inicio = alvo;
        for (Movimento m : movimentos) {
            if (m.mes().isBefore(inicio)) {
                inicio = m.mes();
            }
        }
        BigDecimal divida = BigDecimal.ZERO;
        for (YearMonth mes = inicio;; mes = mes.plusMonths(1)) {
            BigDecimal utilizadas = divida;
            for (Movimento m : movimentos) {
                if (m.mes().equals(mes)) {
                    utilizadas = utilizadas.add(m.horas());
                }
            }
            utilizadas = utilizadas.max(BigDecimal.ZERO);
            divida = utilizadas.subtract(totais).max(BigDecimal.ZERO);
            if (mes.equals(alvo)) {
                return new CotaDoMes(mes, totais, utilizadas, divida);
            }
        }
    }
}

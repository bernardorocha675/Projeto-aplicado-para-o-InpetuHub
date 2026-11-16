package br.com.inpetu.hub.comum;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Como valores aparecem nas telas: reais, datas, horas e Sim/Não (nunca true/false ou null). */
public final class Formatos {

    private static final Locale BRASIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formatos() {
    }

    public static String dinheiro(BigDecimal valor) {
        if (valor == null) {
            return "—";
        }
        return "R$ " + new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(BRASIL)).format(valor);
    }

    public static String dia(LocalDate data) {
        return data == null ? "—" : DIA.format(data);
    }

    public static String diaHora(LocalDateTime quando) {
        return quando == null ? "—" : DIA_HORA.format(quando);
    }

    public static String horas(BigDecimal valor) {
        return valor == null ? "0" : valor.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    public static String simNao(boolean valor) {
        return valor ? "Sim" : "Não";
    }

    /** Texto opcional: vazio ou nulo vira um travessão. */
    public static String ouTraco(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }
}

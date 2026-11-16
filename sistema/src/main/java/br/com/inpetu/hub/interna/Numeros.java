package br.com.inpetu.hub.interna;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;

import br.com.inpetu.hub.comum.RegraException;

/** Leitura dos campos das telas internas: valor inválido vira mensagem para a pessoa, nunca erro técnico. */
final class Numeros {

    private static final String CONFIRA = "Confira a data, o horário ou o número informado.";

    private Numeros() {
    }

    static long inteiro(String valor) {
        if (valor == null || !valor.matches("[0-9]{1,18}")) {
            throw new RegraException(CONFIRA);
        }
        return Long.parseLong(valor);
    }

    static LocalDate data(String valor) {
        try {
            return LocalDate.parse(valor == null ? "" : valor);
        } catch (DateTimeException invalida) {
            throw new RegraException(CONFIRA);
        }
    }

    static LocalTime hora(String valor) {
        try {
            return LocalTime.parse(valor == null ? "" : valor);
        } catch (DateTimeException invalida) {
            throw new RegraException(CONFIRA);
        }
    }
}

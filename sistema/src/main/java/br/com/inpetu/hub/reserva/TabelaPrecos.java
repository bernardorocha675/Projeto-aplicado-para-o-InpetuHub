package br.com.inpetu.hub.reserva;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Tabela de preços ativa: valor por ano, quantidade de turnos e percentual de locação. */
public final class TabelaPrecos {

    private record Chave(int ano, int turnos, int percentual) {
    }

    private final Map<Chave, BigDecimal> valores = new HashMap<>();

    public TabelaPrecos adicionar(int ano, int turnos, int percentual, BigDecimal valor) {
        valores.put(new Chave(ano, turnos, percentual), valor);
        return this;
    }

    public Optional<BigDecimal> valor(int ano, int turnos, int percentual) {
        return Optional.ofNullable(valores.get(new Chave(ano, turnos, percentual)));
    }
}

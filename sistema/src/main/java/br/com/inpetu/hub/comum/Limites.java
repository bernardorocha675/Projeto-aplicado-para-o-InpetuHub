package br.com.inpetu.hub.comum;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Limite de tentativas por janela fixa, só na memória. A janela passa sozinha (nunca trava para sempre) e a memória tem
 * teto: cheio, recusa em vez de crescer. As chaves não devem ter dados pessoais em texto (use um resumo).
 */
public final class Limites {

    static final int MAXIMO_DE_CHAVES = 10_000;

    private record Janela(long ate, int contagem) {
    }

    private final Map<String, Janela> janelas = new HashMap<>();
    private final Clock relogio;

    public Limites(Clock relogio) {
        this.relogio = relogio;
    }

    public synchronized boolean permite(String chave, int maximo, Duration periodo) {
        long agora = relogio.millis();
        if (janelas.size() >= MAXIMO_DE_CHAVES) {
            janelas.values().removeIf(janela -> janela.ate() <= agora);
        }
        Janela atual = janelas.get(chave);
        if (atual == null || atual.ate() <= agora) {
            if (atual == null && janelas.size() >= MAXIMO_DE_CHAVES) {
                return false;
            }
            janelas.put(chave, new Janela(agora + periodo.toMillis(), 1));
            return true;
        }
        if (atual.contagem() >= maximo) {
            return false;
        }
        janelas.put(chave, new Janela(atual.ate(), atual.contagem() + 1));
        return true;
    }
}

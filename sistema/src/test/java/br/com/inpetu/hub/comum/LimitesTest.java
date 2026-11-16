package br.com.inpetu.hub.comum;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

/** Porta do rateLimitDoesNotPermanentlyLockVictim: a janela é fixa e passa. */
class LimitesTest {

    @Test
    void limiteNaoTravaAContaParaSempre() {
        AmbienteDeTeste.Relogio relogio = new AmbienteDeTeste.Relogio();
        Limites limites = new Limites(relogio);
        assertTrue(limites.permite("a", 1, Duration.ofSeconds(1)));
        assertFalse(limites.permite("a", 1, Duration.ofSeconds(1)));
        assertTrue(limites.permite("b", 1, Duration.ofSeconds(1)), "cada chave tem a sua contagem");
        relogio.adiantar(Duration.ofMillis(1001));
        assertTrue(limites.permite("a", 1, Duration.ofSeconds(1)));
    }

    @Test
    void memoriaLimitada() {
        AmbienteDeTeste.Relogio relogio = new AmbienteDeTeste.Relogio();
        Limites limites = new Limites(relogio);
        for (int i = 0; i < Limites.MAXIMO_DE_CHAVES; i++) {
            assertTrue(limites.permite("chave-" + i, 5, Duration.ofMinutes(1)));
        }
        assertFalse(limites.permite("nova", 5, Duration.ofMinutes(1)), "cheio: recusa em vez de crescer");
        relogio.adiantar(Duration.ofMinutes(2));
        assertTrue(limites.permite("nova", 5, Duration.ofMinutes(1)), "as janelas vencidas são descartadas");
    }
}

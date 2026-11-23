package br.com.inpetu.hub.comum;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import javax.sql.DataSource;

/** Banco H2 só na memória, já migrado, e um relógio que o teste adianta quando quer. */
public final class AmbienteDeTeste {

    private AmbienteDeTeste() {
    }

    /** Fora do modo local: sem as contas de demonstração e com o limite de login ligado. */
    public static Ambiente novo() {
        return novo(false);
    }

    public static Ambiente novo(boolean modoLocal) {
        return novo(modoLocal, new Relogio());
    }

    public static Ambiente novo(boolean modoLocal, Clock relogio) {
        DataSource banco = Banco.h2Memoria("teste-" + UUID.randomUUID());
        Migracoes.aplicar(banco);
        return new Ambiente(banco, relogio, modoLocal);
    }

    /** Relógio parado em 12/11/2026 às 10h (Brasília), adiantado só pelo teste. */
    public static final class Relogio extends Clock {

        private volatile Instant agora = Instant.parse("2026-11-12T13:00:00Z");

        public void adiantar(Duration tempo) {
            agora = agora.plus(tempo);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("America/Sao_Paulo");
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return Clock.fixed(agora, zona);
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}

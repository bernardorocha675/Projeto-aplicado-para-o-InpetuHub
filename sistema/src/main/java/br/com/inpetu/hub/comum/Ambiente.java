package br.com.inpetu.hub.comum;

import java.time.Clock;
import java.util.Objects;

import javax.sql.DataSource;

/**
 * O que o sistema recebe ao subir: o banco, o relógio (fixo nos testes) e se está no modo local. No modo local as 5
 * contas públicas de demonstração são restauradas ao iniciar e o login não tem limite de tentativas, como no antigo.
 */
public record Ambiente(DataSource banco, Clock relogio, boolean modoLocal) {

    public Ambiente {
        Objects.requireNonNull(banco, "banco");
        Objects.requireNonNull(relogio, "relogio");
    }
}

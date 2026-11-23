package br.com.inpetu.hub.comum;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/**
 * Aplica as migrations de {@code db/migration} (Flyway). Migration publicada nunca muda: mudança no banco é sempre uma
 * migration nova.
 */
public final class Migracoes {

    private Migracoes() {
    }

    /** Aplica as migrations que faltam e devolve quantas foram aplicadas agora. */
    public static int aplicar(DataSource banco) {
        return Flyway.configure()
                .dataSource(banco)
                .locations("classpath:db/migration")
                .failOnMissingLocations(true)
                .validateMigrationNaming(true)
                .load()
                .migrate().migrationsExecuted;
    }
}

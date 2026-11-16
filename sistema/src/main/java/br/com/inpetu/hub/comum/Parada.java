package br.com.inpetu.hub.comum;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Encerramento em ordem pedido pelo iniciador: quando o arquivo de parada aparece, ele é apagado e o servidor fecha (o
 * banco é liberado e o log registra SERVER_STOPPED). Uma sobra de execução anterior é apagada antes de subir.
 */
public final class Parada {

    private static final long INTERVALO_MS = 300;

    private Parada() {
    }

    /** Apaga a sobra de uma execução anterior (antes de subir o servidor). */
    public static void limpar(Path arquivo) throws IOException {
        Files.deleteIfExists(arquivo);
    }

    /** Vigia o arquivo numa linha de fundo e chama {@code encerrar} uma vez quando ele aparecer. */
    public static Thread vigiar(Path arquivo, Runnable encerrar) {
        Thread linha = new Thread(() -> {
            try {
                while (!Files.exists(arquivo)) {
                    Thread.sleep(INTERVALO_MS);
                }
                Files.deleteIfExists(arquivo);
                encerrar.run();
            } catch (InterruptedException fim) {
                Thread.currentThread().interrupt();
            } catch (IOException naoApagou) {
                encerrar.run();
            }
        }, "inpetu-parada");
        linha.setDaemon(true);
        linha.start();
        return linha;
    }
}

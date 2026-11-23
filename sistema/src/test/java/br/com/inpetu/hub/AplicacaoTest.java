package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import br.com.inpetu.hub.comum.log.CapturaDeLog;

/**
 * O sistema como o iniciador o roda: pasta de dados e porta vindas de fora e encerramento em ordem pelo arquivo de
 * parada (o banco fica livre para a próxima subida).
 */
@Timeout(120)
class AplicacaoTest {

    @TempDir
    Path temporaria;

    private static int portaLivre() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static int saude(int porta) {
        try {
            return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta
                    + "/saude")).timeout(Duration.ofSeconds(2)).build(), HttpResponse.BodyHandlers.ofString())
                    .statusCode();
        } catch (Exception fora) {
            return 0;
        }
    }

    private Thread subir(Path dados, Path parada, int porta, AtomicReference<Throwable> falha) {
        Properties propriedades = new Properties();
        propriedades.setProperty("inpetu.dados", dados.toString());
        propriedades.setProperty("inpetu.parada", parada.toString());
        Thread linha = new Thread(() -> {
            try {
                Aplicacao.executar(new String[]{String.valueOf(porta)}, propriedades);
            } catch (Throwable erro) {
                falha.set(erro);
            }
        }, "aplicacao-de-teste");
        linha.start();
        return linha;
    }

    private static void esperarSaude(int porta) throws InterruptedException {
        long limite = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        while (saude(porta) != 200) {
            assertTrue(System.nanoTime() < limite, "o sistema não ficou pronto");
            Thread.sleep(100);
        }
    }

    @Test
    void arquivoDeParadaEncerraEmOrdemELiberaOBanco() throws Exception {
        Path dados = temporaria.resolve("Dados Ação");
        Path parada = temporaria.resolve("sistema.parar");
        try (CapturaDeLog log = new CapturaDeLog()) {
            for (int vez = 1; vez <= 2; vez++) {
                AtomicReference<Throwable> falha = new AtomicReference<>();
                int porta = portaLivre();
                Thread linha = subir(dados, parada, porta, falha);
                esperarSaude(porta);
                Files.writeString(parada, "parar");
                linha.join(Duration.ofSeconds(30));
                assertFalse(linha.isAlive(), "o sistema não encerrou pelo arquivo de parada (vez " + vez + ")");
                assertEquals(null, falha.get(), "vez " + vez);
                assertFalse(Files.exists(parada), "o arquivo de parada é apagado ao ser atendido");
                assertEquals(0, saude(porta), "a porta foi liberada");
            }
            String texto = log.texto();
            assertEquals(2, texto.split("event=SERVER_STOPPED", -1).length - 1, texto);
            assertTrue(texto.contains("event=SERVER_READY"));
        }
        assertTrue(Files.exists(dados.resolve("inpetu.mv.db")), "o banco fica na pasta de dados");
    }

    @Test
    void paradaDeixadaPorUmaExecucaoAnteriorNaoDerrubaANova() throws Exception {
        Path dados = temporaria.resolve("dados");
        Path parada = temporaria.resolve("sistema.parar");
        Files.writeString(parada, "sobra de antes");
        AtomicReference<Throwable> falha = new AtomicReference<>();
        int porta = portaLivre();
        Thread linha = subir(dados, parada, porta, falha);
        esperarSaude(porta);
        Thread.sleep(1500);
        assertEquals(200, saude(porta), "continua no ar");
        Files.writeString(parada, "parar");
        linha.join(Duration.ofSeconds(30));
        assertFalse(linha.isAlive());
    }

    /** Roda o main num processo à parte, como o iniciador, e devolve tudo o que ele escreveu. */
    private static String rodarMain(int esperado, String dados, String porta) throws Exception {
        Process processo = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Dinpetu.dados=" + dados, "-cp", System.getProperty("java.class.path"), "br.com.inpetu.hub.Aplicacao",
                porta).redirectErrorStream(true).start();
        String saida = new String(processo.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(processo.waitFor(60, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(esperado, processo.exitValue(), saida);
        return saida;
    }

    @Test
    void falhaAoSubirNaoEscreveAPastaDaPessoaNemAMensagemDaExcecao() throws Exception {
        Path arquivo = Files.writeString(temporaria.resolve("Pasta de Fulano"), "não é pasta");
        String saida = rodarMain(1, arquivo.resolve("dados").toString(), "0");
        assertTrue(saida.contains("event=SERVER_START_FAILED failure=FILE_ACCESS"), saida);
        assertFalse(saida.contains("Fulano"), saida);
        assertFalse(saida.contains("Exception in thread"), saida);
        assertEquals(1, saida.split("SERVER_START_FAILED", -1).length - 1, saida);
    }

    @Test
    void argumentoInvalidoNaoEcoaOValor() throws Exception {
        String saida = rodarMain(1, temporaria.resolve("dados").toString(), "porta-de-Fulano");
        assertTrue(saida.contains("event=SERVER_START_FAILED"), saida);
        assertFalse(saida.contains("Fulano"), saida);
    }

    @Test
    void falhaSoltaNumaLinhaDeExecucaoVaiParaOLogSemAMensagem() throws Exception {
        try (CapturaDeLog log = new CapturaDeLog()) {
            Aplicacao.falhasSoltasParaOLog();
            Thread linha = new Thread(() -> {
                throw new IllegalStateException("/home/fulano/segredo");
            });
            linha.start();
            linha.join();
            assertTrue(log.texto().contains("event=TASK_FAILED task=thread"), log.texto());
            assertFalse(log.texto().contains("fulano"), log.texto());
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(null);
        }
    }
}

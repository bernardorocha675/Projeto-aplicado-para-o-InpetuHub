package br.com.inpetu.hub.comum;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class TransacaoTest {

    /** Achado da entrega 12: quem tinha a trava não conseguia outra conexão do conjunto (10) para ler. */
    @Test
    @Timeout(60)
    void muitasGravacoesAoMesmoTempoNaoEsgotamAsConexoes() throws Exception {
        DataSource banco = Banco.h2Memoria("transacao-conjunto");
        Migracoes.aplicar(banco);
        int quantas = 25;
        CyclicBarrier largada = new CyclicBarrier(quantas);
        List<Future<Integer>> resultados = new ArrayList<>();
        try (ExecutorService linhas = Executors.newFixedThreadPool(quantas)) {
            for (int i = 0; i < quantas; i++) {
                resultados.add(linhas.submit(() -> {
                    largada.await();
                    return Transacao.executar(banco, conexao -> {
                        pausar(); // dá tempo de as outras ocuparem o conjunto
                        try (Connection leitura = banco.getConnection()) {
                            return leitura.isValid(1) ? 1 : 0;
                        }
                    });
                }));
            }
            int certas = 0;
            for (Future<Integer> resultado : resultados) {
                certas += resultado.get();
            }
            assertEquals(quantas, certas);
        } finally {
            Banco.fechar(banco);
        }
    }

    private static void pausar() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException fim) {
            Thread.currentThread().interrupt();
        }
    }
}

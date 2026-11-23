package br.com.inpetu.hub.comum;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MigracoesH2Test {

    @Test
    void modeloEDadosIniciaisNoH2EmMemoria() throws Exception {
        ConferenciaDoModelo.conferirTudo(Banco.h2Memoria("migracoes-" + System.nanoTime()));
    }

    @Test
    void bancoEmArquivoNumaPastaComEspacoEAcento(@TempDir Path temporaria) throws Exception {
        Path pasta = temporaria.resolve("Área de Trabalho/InPETU Hub/1/dados");
        ConferenciaDoModelo.conferirTudo(Banco.h2Arquivo(pasta.resolve("inpetu")));
        assertTrue(Files.exists(pasta.resolve("inpetu.mv.db")), "o arquivo do banco fica na pasta pedida");
    }

    @Test
    void caminhoComPontoEVirgulaERecusado(@TempDir Path temporaria) {
        assertThrows(IllegalArgumentException.class, () -> Banco.h2Arquivo(temporaria.resolve("a;b/inpetu")));
    }
}

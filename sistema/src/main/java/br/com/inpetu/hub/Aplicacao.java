package br.com.inpetu.hub;

import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Properties;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.Banco;
import br.com.inpetu.hub.comum.Migracoes;
import br.com.inpetu.hub.comum.Parada;
import br.com.inpetu.hub.comum.Servidor;
import br.com.inpetu.hub.comum.log.Eventos;
import br.com.inpetu.hub.comum.log.Log;
import br.com.inpetu.hub.conta.ContasDemonstracao;

/**
 * Ponto de entrada: {@code java -jar inpetu-hub-sistema.jar [porta]}. Sem porta, usa a 8080. Modo local: banco H2 na
 * pasta de dados ({@code -Dinpetu.dados=<pasta>}, que o iniciador passa; sem ela, {@code target/dados-desenvolvimento})
 * e as 5 contas de demonstração restauradas a cada início. Com {@code -Dinpetu.parada=<arquivo>}, encerra em ordem
 * quando o iniciador cria esse arquivo.
 */
public final class Aplicacao {

    private static final int PORTA_PADRAO = 8080;
    private static final String PASTA_DESENVOLVIMENTO = "target/dados-desenvolvimento";
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private Aplicacao() {
    }

    /** Nenhuma exceção sai daqui: o Java a escreveria com a mensagem, que pode ter a pasta da pessoa. */
    public static void main(String[] argumentos) {
        Log.configurar();
        falhasSoltasParaOLog();
        try {
            executar(argumentos, System.getProperties());
        } catch (Throwable jaRegistrada) {
            System.exit(1);
        }
    }

    /** Uma exceção solta em qualquer linha de execução vai para o log sem a mensagem. */
    static void falhasSoltasParaOLog() {
        Thread.setDefaultUncaughtExceptionHandler((linha, falha) -> Eventos.falhaEmTarefa("thread", falha));
    }

    /** Sobe o sistema e só volta quando ele para (pelo arquivo de parada ou pelo fim do processo). */
    static void executar(String[] argumentos, Properties propriedades) throws Exception {
        String arquivoParada = propriedades.getProperty("inpetu.parada");
        Servidor servidor;
        Ambiente ambiente = null;
        try {
            int porta = argumentos.length > 0 ? Integer.parseInt(argumentos[0]) : PORTA_PADRAO;
            if (arquivoParada != null) {
                Parada.limpar(Path.of(arquivoParada));
            }
            Path dados = Path.of(propriedades.getProperty("inpetu.dados", PASTA_DESENVOLVIMENTO));
            ambiente = new Ambiente(Banco.h2Arquivo(dados.resolve("inpetu")), Clock.system(FUSO), true);
            Migracoes.aplicar(ambiente.banco());
            ContasDemonstracao.restaurar(ambiente);
            servidor = Servidor.iniciar(porta, ambiente);
        } catch (Exception | Error falha) {
            Eventos.inicioFalhou(falha);
            if (ambiente != null) {
                Banco.fechar(ambiente.banco());
            }
            throw falha;
        }
        if (arquivoParada != null) {
            Parada.vigiar(Path.of(arquivoParada), servidor::close);
        }
        Eventos.servidorPronto();
        System.out.println("InPETU Hub pronto em http://" + servidor.endereco() + ":" + servidor.porta() + "/");
        servidor.aguardar();
        Banco.fechar(ambiente.banco());
        Eventos.servidorParado();
    }
}

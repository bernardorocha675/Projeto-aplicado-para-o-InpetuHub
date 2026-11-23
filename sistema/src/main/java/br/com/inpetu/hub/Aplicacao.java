package br.com.inpetu.hub;

import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneId;

import br.com.inpetu.hub.comum.Ambiente;
import br.com.inpetu.hub.comum.Banco;
import br.com.inpetu.hub.comum.Migracoes;
import br.com.inpetu.hub.comum.Servidor;
import br.com.inpetu.hub.comum.log.Eventos;
import br.com.inpetu.hub.comum.log.Log;
import br.com.inpetu.hub.conta.ContasDemonstracao;

/**
 * Ponto de entrada: java -jar inpetu-hub-sistema.jar [porta]. Sem porta, usa a 8080. Modo local: banco H2 na pasta de
 * dados ({@code -Dinpetu.dados=<pasta>}, que o iniciador vai passar; sem ela, {@code target/dados-desenvolvimento}) e
 * as 5 contas de demonstração restauradas a cada início.
 */
public final class Aplicacao {

    private static final int PORTA_PADRAO = 8080;
    private static final String PASTA_DESENVOLVIMENTO = "target/dados-desenvolvimento";
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private Aplicacao() {
    }

    public static void main(String[] argumentos) throws Exception {
        Log.configurar();
        int porta = argumentos.length > 0 ? Integer.parseInt(argumentos[0]) : PORTA_PADRAO;
        Servidor servidor;
        try {
            Path dados = Path.of(System.getProperty("inpetu.dados", PASTA_DESENVOLVIMENTO));
            Ambiente ambiente = new Ambiente(Banco.h2Arquivo(dados.resolve("inpetu")), Clock.system(FUSO), true);
            Migracoes.aplicar(ambiente.banco());
            ContasDemonstracao.restaurar(ambiente);
            servidor = Servidor.iniciar(porta, ambiente);
        } catch (Exception falha) {
            Eventos.inicioFalhou(falha);
            throw falha;
        }
        Eventos.servidorPronto();
        System.out.println("InPETU Hub pronto em http://" + servidor.endereco() + ":" + servidor.porta() + "/login");
        servidor.aguardar();
        Eventos.servidorParado();
    }
}

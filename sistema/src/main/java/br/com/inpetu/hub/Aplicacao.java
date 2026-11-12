package br.com.inpetu.hub;

import br.com.inpetu.hub.comum.Servidor;
import br.com.inpetu.hub.comum.log.Eventos;
import br.com.inpetu.hub.comum.log.Log;

/**
 * Ponto de entrada: java -jar inpetu-hub-sistema.jar [porta]. Sem porta, usa a 8080.
 */
public final class Aplicacao {

    private static final int PORTA_PADRAO = 8080;

    private Aplicacao() {
    }

    public static void main(String[] argumentos) throws Exception {
        Log.configurar();
        int porta = argumentos.length > 0 ? Integer.parseInt(argumentos[0]) : PORTA_PADRAO;
        Servidor servidor;
        try {
            servidor = Servidor.iniciar(porta);
        } catch (Exception falha) {
            Eventos.inicioFalhou(falha);
            throw falha;
        }
        Eventos.servidorPronto();
        System.out.println("InPETU Hub pronto em http://" + servidor.endereco() + ":" + servidor.porta() + "/saude");
        servidor.aguardar();
        Eventos.servidorParado();
    }
}

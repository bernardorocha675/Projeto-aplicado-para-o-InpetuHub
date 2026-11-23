package br.com.inpetu.hub;

import br.com.inpetu.hub.comum.Servidor;

/**
 * Ponto de entrada: java -jar inpetu-hub-sistema.jar [porta]. Sem porta, usa a 8080.
 */
public final class Aplicacao {

    private static final int PORTA_PADRAO = 8080;

    private Aplicacao() {
    }

    public static void main(String[] argumentos) throws Exception {
        int porta = argumentos.length > 0 ? Integer.parseInt(argumentos[0]) : PORTA_PADRAO;
        Servidor servidor = Servidor.iniciar(porta);
        System.out.println("InPETU Hub pronto em http://" + servidor.endereco() + ":" + servidor.porta() + "/saude");
        servidor.aguardar();
    }
}

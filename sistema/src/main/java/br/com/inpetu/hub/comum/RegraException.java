package br.com.inpetu.hub.comum;

import java.io.Serial;

/**
 * Uma regra do negócio recusou o pedido (ex.: horário fora do expediente). A mensagem é escrita no código para a pessoa
 * ler: aparece na tela (HTTP 400), mas nunca vai para o log.
 */
public class RegraException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RegraException(String mensagemParaAPessoa) {
        super(mensagemParaAPessoa);
    }
}

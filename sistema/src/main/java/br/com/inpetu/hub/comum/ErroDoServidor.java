package br.com.inpetu.hub.comum;

import java.nio.charset.StandardCharsets;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.util.Callback;

/**
 * Erros que o Jetty recusa antes de chegar ao sistema (ex.: endereço ambíguo com {@code %2e%2e}). A página padrão do
 * Jetty repete o endereço pedido e o motivo técnico; esta responde um texto fixo, com os cabeçalhos de segurança.
 */
final class ErroDoServidor extends ErrorHandler {

    @Override
    public boolean handle(Request pedido, Response resposta, Callback retorno) {
        Cabecalhos.TODOS.forEach((nome, valor) -> resposta.getHeaders().put(nome, valor));
        resposta.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/plain;charset=UTF-8");
        String texto = resposta.getStatus() == 400
                ? "Pedido inválido. Abra o sistema pelo iniciador.\n"
                : "Não foi possível concluir. Tente novamente.\n";
        resposta.write(true, StandardCharsets.UTF_8.encode(texto), retorno);
        return true;
    }
}

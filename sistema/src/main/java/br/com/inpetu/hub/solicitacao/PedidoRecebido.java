package br.com.inpetu.hub.solicitacao;

import java.time.LocalDateTime;

/** Um pedido ainda não analisado, como a lista de solicitações mostra. */
public record PedidoRecebido(long id, PedidoConferido pedido, LocalDateTime expiraEm, String espaco, String persona) {
}

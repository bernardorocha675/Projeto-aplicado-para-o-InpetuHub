package br.com.inpetu.hub.solicitacao;

import br.com.inpetu.hub.espaco.Espaco;
import br.com.inpetu.hub.reserva.CotaDoMes;
import br.com.inpetu.hub.reserva.DadosDoBanco;
import br.com.inpetu.hub.reserva.Orcamento;
import br.com.inpetu.hub.reserva.Pedido;

/** O que o resumo mostra: espaço, persona, orçamento e a cota do mês. */
public record Estimativa(Pedido pedido, Espaco espaco, DadosDoBanco.PersonaComNome persona, Orcamento orcamento,
        CotaDoMes cota) {
}

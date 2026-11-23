package br.com.inpetu.hub.espaco;

/** Um espaço do Hub. Capacidade é o número de cadeiras; o mobiliário detalha sofás, arquibancada etc. */
public record Espaco(long id, String nome, int capacidade, String mobiliario, boolean disponivel, boolean ativo) {

    /** Recebe pedidos de reserva: ativo e disponível. */
    public boolean reservavel() {
        return ativo && disponivel;
    }

    /** O que a agenda mostra ao lado do nome: o mobiliário ou, sem ele, a capacidade. */
    public String descricao() {
        return mobiliario == null || mobiliario.isBlank() ? "até " + capacidade + " pessoas" : mobiliario;
    }
}

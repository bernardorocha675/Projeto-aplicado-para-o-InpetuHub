package br.com.inpetu.hub.conta;

/** Uma conta da Área Interna, como está no banco (tabela {@code colaborador}). */
record Conta(long id, String nome, String login, String hash, Perfil perfil, boolean ativa, long versao) {

    @Override
    public String toString() {
        // Nunca escrever o hash nem o login em log ou tela por engano.
        return "Conta[" + id + "]";
    }
}

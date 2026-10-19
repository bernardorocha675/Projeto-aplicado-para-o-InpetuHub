# InPETU Hub 0.4.11

Abertura mais confiável do executável e regra de cancelamento.

- Se o iniciador foi fechado à força e o sistema continuou aberto, a próxima abertura encerra esse sistema anterior de forma normal e inicia de novo. Antes, a tela ficava presa em “outra execução está usando os dados”.
- No Linux e no macOS, um pedido de encerramento do sistema operacional ao iniciador agora também para o sistema e finaliza o registro da sessão.
- Uma falha do Java na conferência não é mais apresentada como “outra execução”. No Linux sem idioma configurado, pastas com acentos passam a funcionar.
- Quando a atualização não pode ser instalada (pacote ausente, incompleto ou que não confere), a versão já instalada continua sendo usada, com aviso. Um pacote inválido continua nunca sendo ativado.
- Uma pasta de versão danificada é preservada com outro nome (`.danificada-…`) e a versão é instalada de novo, sem apagar arquivos.
- Reserva aprovada cujo evento já terminou não pode mais ser cancelada; use **Concluir**. A exceção de força maior do gestor, com justificativa, continua disponível. Antes, esse cancelamento cobrava penalidade de cota.
- Os registros de falha passam a indicar o motivo por código (por exemplo, DATA_IN_USE, JAVA_CHECK_FAILED, ORPHAN_APP_STOPPED, UPDATE_SKIPPED).

Dados, reservas, contas, configurações, chave e backups permanecem no mesmo local da família 0.4.x. Não há migration, reset, mudança de dependências ou ativação externa. Windows/macOS/Linux x64/ARM64 e Java privado preservados; Android continua retirado.

Publicação condicionada à matriz obrigatória de RELEASES.md.

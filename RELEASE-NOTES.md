# InPETU Hub 0.5.8

Diagnóstico mais claro: logs com a causa real das falhas e aviso de por que a atualização não foi encontrada.

- **Motivo da verificação de versões.** Quando o iniciador não consegue consultar o GitHub, a tela diz o motivo: limite de consultas do GitHub (“tente de novo em alguns minutos”), sem conexão ou erro do GitHub. Quem já está na versão mais recente vê “Voce ja esta na versao mais recente”. A frase antiga, que juntava “nenhuma atualização” e “consulta offline”, deixou de existir.
- **Releases incompletas ignoradas.** Se a versão mais nova ainda não tiver o pacote do sistema ou o arquivo de conferência, o iniciador escolhe a anterior completa, em vez de ficar preso tentando baixar.
- **Causa real das falhas no log.** Quando o sistema não inicia, o log diz o tipo do problema (porta ocupada, banco em uso, migração, banco, arquivo, memória ou outro) e a cadeia de causas, só com nomes de classe e linhas do código, nunca mensagens, caminhos ou dados.
- **Espera da inicialização não conta como erro.** As respostas “ainda iniciando” viram um único aviso, e o registro de “pronto” informa quantas houve.
- **Agenda aberta não prolonga o login.** A atualização automática da agenda, a cada 30 segundos, não renova mais a sessão de quem está logado; o tempo de inatividade volta a valer.

Dados, reservas, contas, configurações, chave e backups permanecem na mesma pasta (`InPETU-Hub/0.4`); quem está na 0.5.x ou na 0.4.17 recebe esta versão automaticamente, com backup antes da troca. Sem migration nem reset. Os relatórios continuam sem senhas, cookies, tokens, emails, formulários e mensagens livres. Windows/macOS/Linux x64/ARM64 e Java privado preservados. No macOS, o iniciador continua exigindo macOS 13 ou mais recente.

Publicação condicionada à matriz obrigatória de RELEASES.md.

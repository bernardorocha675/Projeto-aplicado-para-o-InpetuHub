# InPETU Hub 0.4.10

Retorno ao aplicativo 0.4.7.

Restaura o visual, a navegação e o comportamento do aplicativo da release 0.4.7, por pedido do responsável. Retira a reprodução do site institucional introduzida na 0.4.8 e os ajustes de interface da 0.4.9.

- Cabeçalho local original: Início, Agenda e Área interna. Retirados os links externos Sobre/Iniciativas/Notícias/Contato e o rodapé institucional.
- Agenda, login, painel, reservas, solicitações e cadastros novamente usam os fontes da tag v0.4.7; mantidas as correções de contatos, datas e confirmação daquela versão.
- Removidos CSS/fontes adicionados pelo redesenho posterior. Nenhuma nova alteração visual ou de regras foi incluída no retorno.
- Regressões adicionais verificam que o menu permanece no aplicativo e a gestão continua acessível após login; smoke confere isso no pacote real.

O número 0.4.10 permite que instalações 0.4.8/0.4.9 recebam a restauração pelo atualizador. Não substitui a tag, arquivos ou release original 0.4.7. Dados, reservas, contas, configurações, chave e backups permanecem no mesmo local da família 0.4.x. Não há migration, reset, mudança de dependências ou ativação externa. Windows/macOS/Linux x64/ARM64 e Java privado preservados; Android continua retirado.

As atualizações de Actions já aprovadas permanecem apenas na infraestrutura da CI. Publicação condicionada à matriz obrigatória de RELEASES.md.

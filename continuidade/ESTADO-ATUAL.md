# Estado atual e ponto de retomada

Registro concluído em **17/10/2026, horário de Brasília**. Confirme novamente fatos que mudam com o tempo ao retomar o projeto.

## Entrega atual comprovada

| Item | Referência |
| --- | --- |
| Repositório | `bernardorocha675/Projeto-aplicado-para-o-InpetuHub` |
| Branch | `main` |
| Release publicada | [v0.4.3](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.3) |
| Commit testado e tag | `b06fcf9d89161a6c512d4e4f3c97c6c28f20ac54` |
| HEAD antes deste fechamento de documentação | `d3856d6b3e86b1775b8f8aaf46db845c49ce21e0` |
| VERSION e POM | `0.4.3` |
| Fontes ativos | `aplicativo/` (Java), `iniciador/` (Go), `receptor/` (Node) |
| Guias | `docs/` |

**A reorganização está concluída e a release foi publicada. Parar aqui e aguardar nova autorização do usuário antes de outra etapa.** A ativação do Render não foi autorizada por esse pedido.

## O que foi feito nesta etapa

O usuário recebeu primeiro o ZIP InPETU-Hub-backup-2026-10-17.zip, com todas as versões dos fontes, histórico completo, sete branches e sete tags. A restauração e a integridade foram verificadas. Esse ZIP registra o estado anterior à reorganização; os binários antigos não estão nele e continuam nas releases. Depois, o usuário autorizou a organização, com uso somente pelo executável e nova release para a entrega.

- Mantida uma única árvore de fontes ativos, separando aplicativo, iniciador e receptor. O Java é uma dependência real do executável, não um sistema alternativo que possa ser apagado.
- Guias reunidos em docs/, com instalação, acessos, atualização, logs, testes, estrutura, receptor e handover atualizados.
- Retiradas da árvore principal as cópias de versões antigas, configurações de VS Code, scripts de configurar/iniciar por terminal, exemplos de ambiente e SQL avulso. Esses conteúdos continuam recuperáveis no histórico e no backup; não se deve inventar tags de versões que nunca foram publicadas.
- Preservados testes de regras, HTTP, contas e backup, ferramentas de compilação/publicação, contratos dos pacotes e atualização da família 0.4.x. Dependabot acompanha o Maven ativo e GitHub Actions. Os workflows antigos foram retirados da árvore atual.
- Regras, migrations, permissões, contas, senhas e dados da instalação foram preservados. A comparação confirmou que o conteúdo Java de produção e seus testes não mudou; o roteiro interno de integração recebeu os ajustes de caminhos/comandos.
- Conferidos os IDs e arquivos das sete releases anteriores: todos foram preservados. Agora há oito releases, incluindo a 0.4.3. Não houve reescrita de histórico, movimentação de tag publicada ou substituição de assets antigos.

Os comandos de código são para desenvolvimento/CI. O usuário final abre o executável e não precisa preparar VS Code, Maven, Go ou Java externo. A pasta de dados continua InPETU-Hub/0.4 no perfil do usuário; reorganizar fontes não muda instalações existentes.

## Validação efetiva da 0.4.3

- [Run aprovado de compilação, testes e publicação](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39020936011): seis ambientes nativos, código, segredos, dependências, CodeQL, relatório de CI e publicação concluídos com sucesso.
- A matriz com JDK 21 passou em Ubuntu 24.04 x64/ARM64, macOS 15 Intel/Apple Silicon, Windows Server 2025 x64 e Windows 11 ARM64.
- Passaram 79 testes Java, 27 testes Go e go vet, cinco testes Node da agenda e sete do receptor/coletor. O roteiro HTTP/perfis/contas/backup usa dados descartáveis. O smoke real valida quinze logins, agenda vazia/livre, cadastro, reinício, troca do iniciador, logs/versões e preservação de banco/chave/sessão.
- A validação local também passou, incluindo o pacote Linux real com JDK 17 compatível com o alvo do POM. Os pacotes publicados foram construídos/testados na CI com JDK 21.
- A primeira execução da reorganização encontrou falta de aspas no argumento Java do PowerShell. Foi corrigido para `java "-Dfile.encoding=UTF-8" --source 17 scripts/Testar.java`. O histórico dessa tentativa foi preservado; a execução corrigida passou. Não remover essas aspas ao alterar o comando.
- [Coletor com os novos caminhos e OIDC real](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39020820074): concluído com sucesso. O destino desativado impede envio de computadores, mas não a validação da identidade do workflow.
- A release contém 12 arquivos carregados: seis ZIPs completos, dois EXEs, dois RUNs, ZIP do código organizado e SHA256SUMS.txt. A tag aponta para o commit testado indicado acima.

Essas verificações não comprovam todos os sistemas/edições ou todos os navegadores nem substituem teste visual de Gatekeeper/SmartScreen. CodeQL tem uma limitação conhecida na interpretação de uma consulta de CSRF; passar o job não garante análise completa nem ausência de falhas. Não houve implantação de produção em MySQL/SMTP/TLS.

O código da release é o instantâneo da tag testada. Este fechamento de estado acompanha main e pode ser mais recente que os documentos dentro do ZIP de código publicado. Não modificar uma release imutável para atualizar esse registro.

## Funcionalidades anteriores preservadas

A 0.4.1 introduziu SEM_ESPACOS na agenda de banco sem espaços ativos/disponíveis. A 0.4.2 acrescentou sessões automáticas ao abrir o executável, versões por evento, recuperação após interrupção, continuidade durante a troca de iniciador e fila de relatórios. Não há botão de iniciar teste para o usuário.

O receptor Node e o coletor estão implementados; o receptor não tem credencial de escrita do GitHub. O coletor usa OIDC restrito ao workflow oficial e GITHUB_TOKEN somente dentro do job. HTTP 202 significa fila, não entrega. Os arquivos locais permanecem até o recibo final; não prometer entrega garantida se o computador estiver fechado e a fila temporária tiver sido perdida.

## Pendente: ativar envio dos computadores do time

`Testes/configuracao.json` continua com `enabled: false` e endpoint vazio. **Logs locais funcionam; o envio do time ao GitHub ainda não foi ativado.** Foram observados cinco relatórios no índice ao fechar esta etapa; são registros de CI, não prova de envio de computadores do time.

Na sessão anterior, a integração do Render estava conectada e exigiu confirmação explícita do workspace antes de criar o serviço. Foi encontrado My Workspace; o identificador deve ser consultado na integração autenticada ao retomar. A pergunta sobre usar esse workspace no plano gratuito continua sem resposta específica. A autorização para organizar o repositório não confirma hospedagem.

Nenhum serviço foi criado nesta etapa e nenhuma URL foi ativada. Ao receber uma nova autorização, conferir serviços e configuração atuais antes de criar algo, para evitar duplicação. As conexões do ChatGPT não passam automaticamente ao Claude Code. O procedimento atualizado está em [OPERACAO.md](OPERACAO.md) e [docs/RECEPTOR.md](../docs/RECEPTOR.md).

## Outras pendências e limites

O responsável operacional, aceite do parceiro, revisão/arquivamento dos relatórios e procedimento de produção ainda precisam de definição. A instalação com cinco contas públicas é de demonstração local e não deve receber dados confidenciais. Não copiar senhas ou segredos para a continuidade.

O PDF de requisitos citado na conversa não foi fornecido nesta retomada; solicitar o documento se uma tarefa depender dele. Não afirmar que todas as melhorias do PDF foram entregues. As tags verificadas da linha antiga são v0.3.0, v0.3.1fix, v0.3.2 e v0.3.3; pedidos antigos de 0.3.4 não comprovam publicação.

Após uma tarefa futura autorizada, atualizar este registro com fatos, commit/release e evidências efetivas. Separar decisões, propostas e conclusões. Não copiar a conversa inteira nem iniciar outra etapa apenas por haver uma pendência listada.

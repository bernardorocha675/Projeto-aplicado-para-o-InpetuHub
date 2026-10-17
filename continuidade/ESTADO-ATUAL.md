# Estado atual e ponto de retomada

Registro conferido em **17/10/2026, horário de Brasília**. É uma fotografia do estado do projeto; confirme novamente os fatos que mudam com o tempo.

## Referências verificadas

| Item | Referência |
| --- | --- |
| Repositório | `bernardorocha675/Projeto-aplicado-para-o-InpetuHub` |
| Branch de referência | `main` |
| HEAD antes da reorganização | `1d7f8b0ff9bb97d1cf74fef1e195b01f674d4a0a` |
| Versão em preparação/validação | `0.4.3` |
| Release publicada | [v0.4.2](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.2) |
| Commit da tag/release | `c9a63fd60ff8583edcceb30287eefc1ca494209f` |
| Fontes ativos | `aplicativo/`, `iniciador/`, `receptor/` |
| Guias | `docs/` |

## Etapa atual: reorganização autorizada

O usuário recebeu o ZIP InPETU-Hub-backup-2026-10-17.zip com todas as versões, histórico completo, sete branches e sete tags; restauração e integridade foram verificadas. Os binários antigos não estão nesse ZIP e continuam nas releases. Depois ele autorizou organizar o repositório, usar somente o executável, preservar releases e criar uma release por entrega. A versão desta etapa é 0.4.3.

Os fontes foram separados em aplicativo/, iniciador/ e receptor/, e os guias em docs/. As cópias antigas de versões, configuração de VS Code e scripts de início/configuração por terminal foram retirados da árvore principal. Testes, empacotamento, dependências e coletor foram ajustados. Banco, contas, senhas, regras e o destino desativado do receptor foram preservados.

A publicação da 0.4.3 depende dos testes e do workflow. Até a confirmação da tag, assets e run, a última release comprovada nesta preparação é 0.4.2. Consultar Actions/release antes de afirmar que a nova entrega está publicada. Ao finalizar esta etapa, **parar e aguardar nova autorização do usuário**. Render não está autorizado nesta etapa.

## Evidências da preparação 0.4.3

Passaram nesta preparação: cinco testes Node da agenda, sete do receptor/coletor, 27 testes Go e go vet, 79 testes Java e o roteiro completo de HTTP/perfis/contas/backup com dados descartáveis. O pacote Linux x64 foi gerado na nova estrutura e passou no smoke real com quinze logins, reinício, troca do iniciador e preservação de banco/chave/sessão. Java de produção e seus testes foram comparados ao conteúdo anterior e permanecem iguais; as adaptações estão nos caminhos e ferramentas. Links locais, YAML, sintaxe dos scripts e verificação de segredos também passaram.

A verificação local usou JDK 17 compatível com o alvo do POM. A publicação usa a matriz com JDK 21 nos seis ambientes nativos. Os resultados de CI e a publicação devem ser conferidos antes do aceite; registrar o run efetivo ao concluir, sem apresentar resultados da 0.4.2 como se fossem da 0.4.3.


## Concluído antes desta reorganização

- Iniciador nativo instala/configura o aplicativo, oferece iniciar/parar e verifica fixes da família 0.4.x.
- A 0.4.1 corrigiu a agenda de banco sem espaços: mostra `SEM_ESPACOS`, sem classificar tudo como ocupado. Espaços e reservas reais mantêm suas regras.
- A 0.4.2 inicia registros automaticamente ao abrir o executável: ações, navegação, erros e versões do aplicativo/iniciador.
- Há logs locais incrementais, finalização ao fechar o iniciador, recuperação de sessões interrompidas na próxima abertura e continuidade durante a troca de iniciador.
- O receptor Node e o coletor GitHub Actions estão implementados e testados. O coletor usa identidade temporária OIDC; a credencial de escrita só existe dentro de Actions.
- A CI já publicou relatórios em `Testes/`. O índice observado contém `teste01`, `teste02` e `teste03`; isso não prova envio de nenhum computador do time.
- Depois da tag, `fba9167620c01326178fb076ada720f169bbd992` corrigiu a identificação dos testes Linux no coletor e acrescentou um teste. Esse ajuste está em `main`, não nos arquivos de código da tag c9a63fd. Não houve mudança no aplicativo Java nem no iniciador Go. Commits posteriores registraram relatórios.

## Evidências da entrega anterior

- [Run da release](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/38987104826): seis ambientes nativos, código, dependências, CodeQL, relatório e publicação concluídos com sucesso.
- [Run após o ajuste do coletor](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/38987925489): concluído com sucesso; os testes Node incluem a identificação de todos os nove jobs.
- [Identidade do coletor após o ajuste](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/38987925560): concluída com sucesso com OIDC real.
- A release contém 12 arquivos: seis ZIPs completos, dois EXEs, dois RUNs, ZIP do código e SHA256SUMS.txt.
- Na entrega foram verificados 79 testes Java, 27 testes Go, cinco testes da agenda e sete testes Node do receptor/coletor após o ajuste. O smoke real cobre quinze logins, perfis, agenda, cadastro, reinício, atualização e preservação de dados/chave.
- O primeiro relatório de CI conserva a falha de log-injection que motivou o ajuste em SafeRoutes; não apague esse histórico para apresentar apenas sucessos. As execuções posteriores passaram.

Esses são resultados das execuções indicadas, não testes executados novamente para produzir estes documentos. A análise CodeQL registrou uma limitação de interpretação de uma consulta de CSRF; passar o job não comprova análise completa nem ausência de vulnerabilidades. Não há comprovação de teste visual em todos os navegadores, Gatekeeper/SmartScreen ou implantação de produção.

## Pendente: ativar envio dos computadores do time

`Testes/configuracao.json` continua exatamente com `enabled: false` e `endpoint` vazio. Portanto **os registros locais funcionam; o envio do time ao GitHub ainda não foi ativado**. A interface informa a pendência e mantém os arquivos locais.

Na sessão anterior, a integração do Render estava conectada, mas exigiu confirmação explícita do workspace antes da criação do serviço. Foi encontrado **My Workspace**. O identificador deve ser consultado na integração autenticada ao retomar. A pergunta enviada foi se o usuário confirma esse workspace, no plano gratuito, para hospedar somente o receptor. **Ainda não houve resposta a essa pergunta no histórico até este registro.** O pedido de criar esta área não é confirmação de hospedagem.

Nenhum serviço receptor foi criado por aquela etapa; não há URL real registrada. Ao retomar, confira a configuração e os serviços atuais antes de criar algo, para evitar duplicação. As conexões do ChatGPT não passam automaticamente para o Claude Code.

Próximo passo, se autorizado: publicar o receptor, validar saúde e proteção da fila, ativar a URL real e conferir uma sessão de ponta a ponta até o recibo final no GitHub. Procedimento e critérios: [OPERACAO.md](OPERACAO.md).

## Outras pendências e limites

- Não transformar a demonstração com contas públicas em instalação de produção com dados confidenciais.
- O aceite do parceiro, o responsável operacional, a revisão/arquivamento dos relatórios e o procedimento de produção ainda precisam de definição.
- O PDF de requisitos mencionado na conversa não foi fornecido nesta retomada. O handover não afirma que todas as melhorias do PDF foram entregues; solicite o documento se uma tarefa depender dele.
- Não inferir que os pedidos antigos de 0.3.4 foram publicados: as tags verificadas da linha 0.3 são v0.3.0, v0.3.1fix, v0.3.2 e v0.3.3.

## Como atualizar este registro

Após uma tarefa, substitua o estado que mudou, acrescente links/SHAs dos testes e registre a pendência seguinte. Separe fatos comprovados, decisões do usuário e propostas. Nunca copie credenciais ou a conversa inteira. Use o commit que antecede o registro, sem inventar o SHA do próprio commit ainda não criado.

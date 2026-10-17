# Continuidade do projeto

Esta área permite que outra pessoa ou ferramenta continue o InPETU Hub sem depender do histórico do chat. Reúne decisões, estado da entrega, referências de código e a próxima tarefa pendente. Não é uma transcrição completa da conversa nem substitui o código, o handover ou os resultados de testes.

## Como começar no Claude Code

1. Clone ou atualize este repositório e abra o Claude Code na raiz do projeto.
2. O arquivo `CLAUDE.md` contém as instruções do projeto e importa `continuidade/ESTADO-ATUAL.md`. Confira os arquivos carregados usando `/memory`.
3. Para conferir a compreensão antes de trabalhar, envie:

> Leia o CLAUDE.md e os documentos da pasta continuidade. Resuma o objetivo, a versão atual, as decisões que devem ser preservadas, o que já foi testado e o que ainda está pendente. Não altere nada nessa primeira leitura.

4. Depois, informe a tarefa desejada. A pendência do Render não está autorizada automaticamente por este documento.

## Onde está cada informação

| Arquivo | Conteúdo |
| --- | --- |
| [CLAUDE.md](../CLAUDE.md) | Instruções de trabalho e decisões essenciais para o agente. |
| [ESTADO-ATUAL.md](ESTADO-ATUAL.md) | Entrega verificada, evidências, bloqueios e próximo passo. |
| [CONTEXTO.md](CONTEXTO.md) | Produto, histórico resumido, arquitetura e decisões do projeto. |
| [OPERACAO.md](OPERACAO.md) | Mapa do código, comandos de teste, publicação e retomada do envio. |
| [Handover da linha 0.4](../docs/HANDOVER.md) | Detalhes técnicos e limitações da versão. |

## Limites da transferência

O Claude recebe os documentos que conseguir ler no checkout. Ele não recebe automaticamente esta conversa, anexos antigos, sessões autenticadas, integrações do ChatGPT, arquivos privados de cada computador ou resultados futuros de CI. Quando um PDF ou requisito não estiver no repositório, forneça o documento antes de pedir sua implementação.

Mantenha esta área no Git. Ao encerrar uma etapa, registre o estado e as evidências, sem dados pessoais ou segredos. Se houver divergência, confira o código, a release e Actions antes de agir. Documentos de continuidade acompanham `main`; a release já publicada continua imutável.

Referência do formato: [documentação oficial de memória e CLAUDE.md](https://code.claude.com/docs/en/memory), consultada em 16/10/2026. O arquivo orienta o agente; não substitui permissões e controles da ferramenta.

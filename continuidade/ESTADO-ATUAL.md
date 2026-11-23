# Estado atual

Atualizado em 23/11/2026. **Mantenha este arquivo em uma página:** troque o que mudou em vez de acrescentar. Os registros anteriores estão no [histórico](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/blob/7129fccce3aedc9590a69b2ab4a5655a5a89910f/continuidade/ESTADO-ATUAL.md).

## Onde estamos

- **1.0.0-alpha.1** publicada como pré-release em 23/11/2026, para Windows x64 e Linux x64: [release](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v1.0.0-alpha.1), run 45212853659, tag em `1e28f5a`.
- A **0.5.12** continua a “Latest” nas Releases; código e fluxo da 0.5 intactos. O iniciador antigo ignora a linha 1.x.
- As entregas 1 a 12 estão na `main`. O README mostra só a 1.0.

## Evidência

- Entrega 12 (Linux x64): 213 testes Java (1 pulado: MySQL), 64 do iniciador com o jar real, 10 dos scripts, axe sem violações em 19 telas e estados.
- CI: pacotes de Linux e Windows aprovados, com a troca de versão na fumaça; migrações com MySQL 8.4.
- Depois de publicar: somas conferidas, 385 arquivos das releases antigas iguais, fumaça do pacote Linux baixado e instalação pelo GitHub real.
- Restauração de um backup conferida no Linux, com o jar atual (23/11/2026).

## Pendências

- Time testar a alpha.1, primeiro num Windows. Correções vão para a alpha.2.
- Decisões abertas no [plano](../docs/PLANO-REESCRITA.md#o-que-falta).
- Agente de privacidade (rodada completa) e scan do Claude Security: não feitos. A configuração do Claude Security está só no computador do responsável, sem commit.
- `windows-11-arm` falhou uma vez no teste da 0.5 (run 45197291212) e passou antes e depois; causa não identificada.
- `.github/SECURITY.md` cita a busca de cidades, que a 1.0 não tem.
- Dependabot abertos: #54 e #56 (CodeQL, precisam entrar juntos), #57 e #70. PR #53 aberto desde 11/11.

## Não fazer sem novo pedido

Publicar, mexer em tags ou releases, apagar dados, retomar Android, ativar o envio de relatórios (Render) ou mexer na 0.5.

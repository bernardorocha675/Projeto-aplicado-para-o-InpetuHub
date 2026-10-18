# Revisão dos PRs de GitHub Actions — 18/10/2026

O responsável mostrou os PRs abertos #5, #6 e #7 e autorizou verificar e publicar tudo. Base da preparação: `90c997827fbe979d32e066e675eb916c9f1e5c3d`. Main em `215e57d2055faeb4a6b0455436a71a9f26a9115d`; checkout limpo. Foram lidas as instruções, continuidade, handover e RELEASES.md. As três propostas modificam somente `.github/workflows/release-v0.4.yml`.

| PR | Alteração | Verificação e impacto |
| --- | --- | --- |
| [#5](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/pull/5) | download-artifact 5.0.0 → 8.0.2; SHA `9000827ccba6bdab643e8b6fd33ac0654aef8333` | Tag oficial conferida. `pattern`, `path` e `merge-multiple` continuam suportados; o contrato mantém artefatos do mesmo run. Checks históricos de seis alvos aprovados. O download real será comprovado no job de publicação de main. |
| [#6](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/pull/6) | setup-go 6.5.0 → 7.0.0; SHA `b7ad1dad31e06c5925ef5d2fc7ad053ef454303e` | Tag oficial e action.yml conferidos. Entradas de versão/arquitetura/cache mantidas. Go continua fixado em 1.27.2; checks históricos de seis alvos aprovados. |
| [#7](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/pull/7) | setup-java 6.0.1 → 6.1.0; SHA `b843ebf9b913146b38da2eaa7aeddf76074ae1ad` | Tag oficial, README e action.yml conferidos. Temurin/JDK 21 e arquiteturas preservados. Houve falhas antigas no roteiro HTTP/backup dos dois alvos Windows, após instalação Java concluída. A integração atual exige uma matriz nova completa. |

As três ações usam Node 24 nos runners hospedados. Não houve alteração de dependências do aplicativo, versão do JDK/Go, permissões, validação de segredos, CodeQL, gates ou regras de negócio. Os merges preservam os commits originais do Dependabot e o histórico. Nenhum teste foi removido ou enfraquecido.

## Falhas históricas do PR #7

No run [39020871855](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39020871855), os jobs Windows x64 e ARM64 falharam em “Executar testes de regras, HTTP e backup”. As etapas anteriores, incluindo setup-java, passaram. O detalhe dos logs antigos retornou HTTP 404 no acesso disponível. Não foi possível estabelecer a causa raiz histórica; não atribuir a falha à instalação do Java nem declarar correção de uma causa que não foi reproduzida.

A evidência relevante para publicar será o novo run de main sobre o código atual integrado, que repetirá os testes nos seis sistemas/arquiteturas. Se houver falha, investigar antes da release. As execuções antigas permanecem no histórico.

## Estado da entrega

PRs integrados localmente com merges sem conflitos. YAML e contrato dos seis alvos/gates conferidos; 19 testes Node e check local de segredos aprovados. A publicação da 0.4.8 dependerá do run obrigatório de main, incluindo relatório de CI e download dos artefatos pelo novo download-artifact. O resultado final acompanhará VALIDACAO.md e continuidade/ESTADO-ATUAL.md.

O acesso à referência oficial da agenda continua bloqueado. Após informado desse limite, o responsável autorizou a publicação da implementação pelas capturas; a pendência visual permanece documentada, sem alegar equivalência integral. Nenhum banco, chave, backup ou release anterior foi alterado.

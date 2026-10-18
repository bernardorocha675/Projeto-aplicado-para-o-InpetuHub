# Revisão desktop — primeiro lote 0.4.6

Base: `64487c7e17e254a5c743e16d375ce3eb2020ee9a`, checkout limpo na branch local `work`, remoto main no mesmo commit. Não havia AGENTS.md no workspace. Foram lidos CLAUDE.md, continuidade, handovers e RELEASES.md. A página pública da CI mostrou o run desktop 39059503236 aprovado; última estável observada: v0.4.5. API GitHub bloqueada pelo proxy e gh sem autenticação utilizável; leitura por páginas públicas/Git HTTPS.

## Bugs confirmados e corrigidos

| Arquivo | Impacto e reprodução anterior | Correção e evidência |
| --- | --- | --- |
| `iniciador/model.go`, `prepareConfig` | Com banco existente e chave ou `.env.local` ausente, gerava novos arquivos silenciosamente. Outra chave deixa de corresponder aos dados protegidos; outra senha H2 impede abrir o banco. `TestMissingPrivateConfigurationNeverReplacesExistingInstallation` falhou antes do patch nos dois subcasos. | Valida o conjunto antes de escrever; com dados existentes, recusa recriar arquivos ausentes e orienta restaurar backup completo. Recusa configuração não regular/atalhos. Regressão confere banco e outro arquivo byte a byte e ausência de recriação. Instalação nova continua automática. |
| `iniciador/update.go`, `downloadRelease` | Erro na leitura do corpo do ZIP bloqueava `installLatest`, apesar de existir versão íntegra instalada. A recuperação existente cobria conexão/status HTTP. `TestTruncatedDownloadKeepsInstalledVersion` reproduziu `unexpected EOF`. | Classifica interrupção de transferência como indisponibilidade de rede e reutiliza o pacote instalado. Regressão exige versão anterior pronta e nenhuma instalação parcial. Checksum inválido, excesso de tamanho e erro de disco continuam recusados. |

Sem migrations, reset, troca de contas/senhas, alteração de cotas/valores, dependências ou família do atualizador. Testes usam dados descartáveis.

## Retirada do Android

Retirados fontes, recursos, manifests, instrumentação, scripts de build/assinatura/publicação, certificado **público**, workflow e três guias exclusivos. Nenhum APK estava rastreado. Nenhuma chave privada ou backup de recuperação estava entre os arquivos retirados. README, estrutura, handover e continuidade apresentam somente desktop. Histórico Git, tags e releases anteriores são preservados; a prerelease Android permanece histórica.

## Suspeitas e sugestões para outra etapa

Estas observações não são bugs reproduzidos nem vulnerabilidades validadas:

- `iniciador/app.go`, `checkStopped`: a conferência do lock herda variáveis Java, enquanto `startBundle` usa `cleanEnvironment`. Investigar falsas indicações de instalação em uso causadas por opções Java de outro projeto.
- `iniciador/model.go`, `atomicWrite`: o fallback Windows remove o destino após falha de rename. Investigar falhas entre remoção e nova gravação em runner Windows; não inferir perda de dados a partir de Linux.
- `iniciador/app.go`, `openBrowser`: usa Start sem Wait. Verificar coleta de processos auxiliares nos três sistemas antes de mudar.
- `ReservaService` e `CotaService` mantêm condicionais de estados pendentes dentro de ramos restritos a aprovadas. Simplificação possível sem alterar a regra de negócio; fora deste lote.

A revisão incluiu instalação, download/extração, seleção de pacote, processo Java, lock, backup, reservas/disponibilidade/cotas e validação existente. Não é auditoria exaustiva nem afirmação de ausência de vulnerabilidades. Dependências e CodeQL exigem seus gates reais. Render, SMTP e envio externo não foram ativados.

## Validação

Resultados e publicação em `VALIDACAO.md` e `continuidade/ESTADO-ATUAL.md`. A matriz obrigatória mantém seis runners; testes locais Linux não comprovam outros sistemas. Aguardar autorização para outro lote.

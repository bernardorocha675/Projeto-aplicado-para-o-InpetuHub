# Revisões do desktop

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


## Segunda revisão — lotes 0.4.11 e 0.4.12

Análise de 19/10/2026 sobre a 0.4.10. O lote 1 (0.4.11) corrigiu o Java deixado por iniciador encerrado à força, a conferência de parada que confundia falha da JVM com dados em uso, pastas acentuadas no Linux sem locale, atualizações que bloqueavam a versão instalada e o cancelamento de eventos já terminados. O lote 2 (0.4.12) trata os itens abaixo.

| Item | Resultado da validação | Tratamento na 0.4.12 |
| --- | --- | --- |
| Download com prazo total de 5 min para pacotes de ~110 MB | Confirmado no código: o prazo do `http.Client` cobre a transferência inteira (exige ~3 Mbit/s). | Sem prazo total; prazos de conexão, TLS e cabeçalho, mais interrupção após 60 s sem dados. Progresso em MB na tela. Falha não se repete ao clicar Iniciar na mesma execução. |
| Erros antes da tela invisíveis (Windows `windowsgui`, app do macOS) | Confirmado no código: só saída de erro. | Aviso nativo (MessageBox, alerta do macOS, zenity/kdialog/xmessage no Linux); não aparece com `--headless`. |
| Aplicativo aceita qualquer cabeçalho Host (DNS rebinding) | **Reproduzido** no pacote 0.4.11: `Host: atacante.example` recebeu HTTP 200. | Perfil local recusa nomes diferentes de 127.0.0.1/localhost/::1 com HTTP 400; teste Java e smoke. Produção não muda. |
| Pastas `.download-*`/`.stage-*` após interrupção | Confirmado: só eram removidas pelo próprio processo. | Removidas na próxima instalação, com o lock do iniciador. Pastas `.danificada-*` continuam preservadas. |
| WARN de envio repetido a cada minuto sem internet | Reproduzido em teste: 3 tentativas, 3 eventos. | Registra só a mudança para falha; volta a registrar após um envio bem-sucedido. |
| Limite de 60 consultas/h da API sem login | Confirmado: abrir e iniciar consultavam duas vezes. | Resultado reutilizado por 10 min na mesma execução. Redes compartilhadas ainda podem atingir o limite; a versão instalada continua utilizável. |
| `atomicWrite` no Windows remove o destino antes de renomear | Confirmado no código; perda só se a segunda troca falhar. | Até 10 novas tentativas antes do último recurso anterior. Não testado em Windows real além da matriz. |
| `openBrowser` sem `Wait` | Confirmado: processos auxiliares encerrados ficavam pendentes no Linux/macOS. | Aguarda o auxiliar em segundo plano. |
| `jlink --compress=2` obsoleto | Aviso confirmado no JDK 21. | `--compress=zip-6`, equivalente. |

Mantidos sem mudança, com justificativa: corrida curta entre escolher a porta e o Java ocupá-la (rara; a falha aparece como APP_NOT_STARTED e basta iniciar de novo); crescimento de `backups/` e `logs/sessoes/` (contêm dados do usuário; apagar exige decisão do responsável); limite de 15 envios públicos/15 min por IP no modo local (proteção mantida por decisão de não remover controles); nomes de usuário Windows fora do cp1252 (sem ambiente para validar; a matriz cobre acentos do português); backup criptografado da assinatura Android publicado na prerelease (decisão do responsável; não alterado); releases não marcadas como imutáveis no GitHub (configuração do repositório, recomendada ao responsável); ausência de `govulncheck` e de verificação do JRE embarcado na CI (melhoria opcional para outro lote).

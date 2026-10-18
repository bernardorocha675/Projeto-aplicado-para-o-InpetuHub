# Identidade institucional — 0.4.8 em preparação

Base `215e57d2055faeb4a6b0455436a71a9f26a9115d`. A release pública atual continua 0.4.7. A preparação pelas três capturas foi implementada; o acesso a https://inpetuhub.sites.ufsc.br/agenda/ retornou HTTP 403 pela rede do ambiente. Não há evidência de equivalência integral da marca, fontes, links, submenus ou redes sociais. Dependências concretas em [IDENTIDADE-INSTITUCIONAL.md](IDENTIDADE-INSTITUCIONAL.md).

Validação local Linux x64, 18/10/2026:

- 118 testes Java: zero falhas, erros ou ignorados. Inclui visitante obtendo os quatro WOFF2 locais com cabeçalho binário válido. Classificação ASSET das fontes coberta em LoggingTest.
- Testar.java: VERIFICAÇÕES AUTOMÁTICAS APROVADAS, relatório de 18:41:01 UTC. Roteiro de regras, segurança, HTTP/perfis, persistência, backup cifrado/restauração e contas em dados descartáveis. Uma tentativa concorrente foi corretamente recusada pelo lock do executor; foi aguardada a rodada original, sem enfraquecer o bloqueio.
- 33 testes Go com detector de concorrência e go vet aprovados. 19 Node: dez agenda, dois administração e sete receptor/coletor. O regressor de falha de atualização não passou quando removida somente a limpeza dos links antigos; passou com a correção. Todos os testes existentes preservados.
- JAR final recompilado após os ajustes de CSS/JS. Chromium 154 em pt-BR: fluxo real login → cadastro → agenda → reserva → erro/correção → revisão/edição → conferência manual, obrigatórios e limites HTML aprovados, zero erros JS. A largura 390px mantém os sete dias e textos dentro da tela.
- Comparação visual local separada com resposta sintética interceptada, somente no navegador: desktop 1919px, 1024px, 768px e 390px sem transbordamento horizontal, seleção/detalhes, ferramentas expansíveis e quatro fontes carregadas. Zero requisições externas da página. Na largura 1919px: cabeçalho 84px, painel x=66,6/y=144, altura 696,2px, colunas 699,4/1071,3px, grade y=288 e rodapé y=900,2. Esses valores corroboram proximidade com a captura; não demonstram igualdade de DOM/estilo com o site bloqueado.

Pacote final Linux x64 com Java privado e smoke do executável real aprovados: instalação, 15 logins, permissões, agenda, cadastro, reinício, troca do iniciador, banco/chave preservados e encerramento. Código testado `12bdb6b169b4020692deb88584751ac80757203a`, enviado para `release-v0.4`. [Run 39155638958](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39155638958): nove jobs aprovados — Ubuntu 24.04 x64/ARM64, macOS 15 Apple Silicon/Intel, Windows Server 2025 x64, Windows 11 ARM64, fonte/segredos, dependências e CodeQL. Cada alvo nativo executou testes, roteiro HTTP/backup, empacotamento e smoke. Relatório público em Testes e publicação de release não foram executados: são restritos a main. Não confundir estes nove checks com uma release publicada ou com o pipeline de publicação completo. CSS/JS/templates e quatro fontes do JAR foram comparados byte a byte com os fontes; o JAR no ZIP final possui o mesmo SHA-256. Conferidos novamente os links/digests das 12 releases históricas e os 12 assets/SHA256SUMS da 0.4.7; todas as 13 tags permanecem idênticas ao inventário anterior. Nenhuma tag 0.4.8 criada. Não declarar novos executáveis publicados. A publicação em main depende da conferência oficial e dos gates obrigatórios. Sem migrations, reset, alteração de contas/regras ou ativação externa. Android segue retirado; histórico e releases anteriores preservados.

---

# Validação do relatório caçabugs — 0.4.7

Base `efe162a1ff9b1296a22b8ab4fff61174ca70af19`. Release [0.4.7](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.7) publicada pelo [run 39152880226](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39152880226), com 11 jobs aprovados. Tag no commit `992168e3ceb39f196b2153aa5e291fbae0e2f196`. Foram conferidos os 12 assets e SHA256SUMS contra os digests públicos; as 12 tags/releases anteriores mantêm os arquivos e digests registrados antes da publicação. Escopo autorizado: todos os itens restantes do DOCX. Detalhes em [CORRECOES-RELATORIO.md](CORRECOES-RELATORIO.md).

Execução local Linux x64 em 18/10/2026:

- 117 testes Java, zero falhas/erros/ignorados; 18 novos cobrem contatos, ISO em pt-BR, agenda → reserva, erro/correção, resumo/edição, código ativo/expirado/manual/ausente, rollback de edição, registros históricos e limites de data/virada anual. A execução inicial dos primeiros 13 regressores teve 12 falhas antes do patch; o teste de erro técnico já passava.
- Roteiro Testar.java aprovado: regras, segurança, concorrência, HTTP, contas, persistência, backup cifrado/restauração e limpeza de dados descartáveis. O JAR final também foi conferido contra os recursos CSS atuais após o ajuste visual.
- 33 testes Go com detector de concorrência e go vet aprovados. Node: sete agenda, dois administração e sete receptor/coletor, todos aprovados. O teste da agenda usa relógio fixo para não depender do dia de execução.
- Chromium 154, locale pt-BR e fuso America/Sao_Paulo: fluxo real de login/cadastro de espaço, agenda → reserva, rejeição dos exemplos de contato, limites HTML de data, erro de horário/correção sem perda de data/contatos, revisão/edição, conferência manual sem campo de código e indicação dos obrigatórios. Repetido no JAR final; zero erros JavaScript. Verificados desktop 1440px e largura 390px, incluindo limites visíveis da grade e dos textos após corrigir o corte observado na primeira captura.
- Pacote final Linux x64 com Java privado e smoke do executável real aprovados: 15 logins, permissões, agenda vazia/livre, cadastro, reinício, troca real do iniciador e banco/chave preservados. A CI aprovou fonte/segredos, dependências e CodeQL, além de Ubuntu 24.04 x64/ARM64, macOS 15 Apple Silicon/Intel, Windows Server 2025 x64 e Windows 11 ARM64. Cada alvo executou o roteiro de testes, empacotamento e smoke nativo. Evidência pública em Testes/teste10-2026-10-18_15-09-45-adeb027e/log.txt.

Sem migrations, reset, alteração de contas/senhas/preços/cotas, dependências, Android ou ativação externa. SMTP real e outros navegadores/dispositivos não foram testados; contatos são validados por formato, não por existência. Os preços de anos futuros continuam dependendo de cadastro do gestor. O rascunho de edição dura enquanto a sessão for válida.

---

## Histórico da 0.4.6

# Validação do primeiro lote 0.4.6

Execução local em Linux x64, 18/10/2026: JDK 21 completo, Go 1.27.2, Maven 3.9.9 e Node 24.19.0. Release [0.4.6](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.6) publicada pelo [run 39150431777](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39150431777), 11 jobs aprovados. Tag no commit `5d6e2df8b3d46da9041df97df9504627a0e891cb`. Confirmados 12 assets e SHA256SUMS contra digests públicos. As 11 tags anteriores e os links/digests dos arquivos das 11 releases anteriores permanecem iguais ao inventário registrado antes da publicação.

A matriz executou com sucesso Ubuntu 24.04 x64/ARM64, macOS 15 Apple Silicon/Intel, Windows Server 2025 x64 e Windows 11 ARM64. Cada runner executou testes Go/Java, roteiro HTTP/backup, empacotamento e smoke do binário real; fonte/segredos, dependências, CodeQL, relatório e publicação também passaram. Evidência pública resumida em `Testes/teste09-2026-10-18_14-37-21-affaf8f1/log.txt`.

- 99 testes Java: zero falhas, erros ou ignorados. `java -Dfile.encoding=UTF-8 --source 17 scripts/Testar.java` terminou com VERIFICAÇÕES AUTOMÁTICAS APROVADAS, incluindo HTTP/perfis, contas, persistência, backup real/restauração e limpeza dos dados descartáveis.
- 33 testes Go (mais dois subcasos da configuração), zero falhas; `go test -race ./...` e `go vet ./...` aprovados. As duas regressões principais falharam antes do patch. Teste adicional distingue erro de disco de erro de leitura da rede.
- Node: cinco testes da agenda, dois da interface administrativa e sete do receptor/coletor aprovados.
- Empacotamento Linux x64 criou o ZIP 0.4.6 com Java privado. Smoke do binário real aprovado: configuração, 15 logins, permissões, agenda vazia/livre, cadastro, reinício, troca real do iniciador, preservação da chave/banco e encerramento. Sem Java externo no processo do usuário.
- Links Markdown locais conferidos sem referências quebradas; YAML dos workflows remanescentes interpretado; `git diff --check` aprovado. Nenhum APK rastreado. Nenhum guia ativo oferece Android.
- Consulta de dependências: Maven resolveu a lista, mas o check OSV não conseguiu consultar a API pela rede deste ambiente e retornou erro. Não é aprovação nem prova de ausência de vulnerabilidades. O gate de dependências consultou OSV com sucesso na CI; CodeQL e fonte/Gitleaks também foram aprovados. Não foram substituídos por checks triviais.

Compilação cruzada do iniciador também aprovada localmente para os outros cinco alvos. Windows, macOS e ARM64 foram executados somente nos runners nativos da CI. Nenhum teste local comprova esses alvos, outras distribuições, políticas corporativas, assinatura comercial ou notarização. Não foram ativados Render, SMTP ou envio externo. Situação da publicação e commit em continuidade/ESTADO-ATUAL.md.

---

## Evidências históricas da 0.4.5

# Validação — 0.4.5

Fix de logs publicado na [v0.4.5](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.5), código testado e tag `1fac5b0bf5f432db48f85f63181c037859817f23`. O [run 39028486594](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39028486594) concluiu os 11 jobs com sucesso. Conferidos os 12 arquivos carregados; as nove releases anteriores preservaram IDs, nomes, arquivos/tamanhos/digests.

Validação local aprovada: 99 testes Java (97 no roteiro completo, mais dois de SMTP simulados), 30 testes Go e go vet, cinco testes Node da agenda, dois da interface administrativa e sete do receptor/coletor. Passaram o roteiro completo HTTP/perfis/contas/backup, empacotamento e smoke do executável Linux real.

Regressões novas cobrem erro inesperado com página simples/requestId/tipo/pontos no log sem mensagem livre; regras e integridade retornando 400/409; data inválida registrada sem mostrar erro de conversão; parâmetro ausente 400, recurso ausente 404 e autorização 403. SMTP é simulado, sem envio real. Go cobre gravação final incompleta sem upload prematuro, recuperação na abertura, reparo de sessão legada preservando conteúdo e diagnóstico de envio sem segredo. O smoke cria um erro controlado na reserva do executável real e confere sua presença no relatório sanitizado.

A matriz foi aprovada nos seis ambientes: Ubuntu 24.04 x64/ARM64, macOS 15 Intel/Apple Silicon, Windows Server 2025 x64 e Windows 11 ARM64, com os gates de fonte/segredos/dependências/CodeQL/relatório/publicação também aprovados. Mantém JDK 21 no pacote, alvo Java 17 e Go 1.27.2.

O transporte e recibos são testados com servidor simulado. Nenhum workspace Render foi selecionado, serviço criado ou endpoint ativado. O envio real dos PCs não foi comprovado e continua pendente da confirmação do workspace.

Os testes não comprovam todos os navegadores/edições, queda física de energia/disco ou cada falha interna do navegador. Não há implantação MySQL/SMTP/TLS de produção. Mantém-se a limitação conhecida de interpretação de consulta CSRF no CodeQL. Outros itens do PDF permanecem pendentes.

O smoke real com erro controlado, seus eventos/versões e a ausência do conteúdo do formulário passou nos seis ambientes. Este fechamento acompanha main; os documentos no ZIP de código são o instantâneo anterior à CI. A release imutável não foi substituída.

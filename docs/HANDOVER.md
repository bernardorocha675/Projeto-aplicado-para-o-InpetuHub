# Handover técnico — InPETU Hub 0.4.16

Base desta reorganização: `1d7f8b0ff9bb97d1cf74fef1e195b01f674d4a0a` (main após 0.4.2 e continuidade). A release 0.4.2 permanece no commit `c9a63fd60ff8583edcceb30287eefc1ca494209f`. Base da linha 0.4: `12abbbcf9956ea68804229ac37ea527fb181f9df` (0.3.3). Entrega acadêmica de Projeto Aplicado do UniSENAI com o InPETU Hub. Equipe: Bernardo Rocha, João Mussiol, João Feitosa e Guilherme Batista. O responsável operacional e o aceite do parceiro ainda precisam ser definidos.

## Fix 0.4.16 — lote 2 do Caça bugs 2 (calendário) — publicada e verificada

A [0.4.16](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.16) foi publicada e verificada em 19/10/2026 pelo [run 39330445496](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39330445496), na primeira tentativa, após o PR [#18](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/pull/18) (matriz verde de primeira). Tag/código testado `ae99fe07aca99b95093d2f7e139a8eee4572e55d` (merge em main). Conferidos os 12 assets e os 11 checksums contra digests públicos; v0.4.16 é a latest; as 21 releases anteriores mantêm arquivos, tamanhos e digests; links do README respondem. Relatório da CI: `Testes/teste23-2026-10-19_18-49-14-2e3f29e6/log.txt`.


Autorizado em 19/10/2026. `agenda.js`: o dia recebe `.has-events` e um ponto (`.event-dot`) quando algum espaço tem `ocupados`; sem atividade, só o número; a situação (sem espaços, sem horários livres, feriado) fica no `aria-label` e nos detalhes ao clicar. Título do mês sem “de”. `agenda.html`: a legenda Livre/Parcial/Ocupado vira “Clique em um dia marcado para ver os eventos.”; `style.css` com o visual da referência. Testes locais (Linux x64, 19/10/2026): 126 Java (página pública confere o texto “Clique em um dia marcado”), Node da agenda (10, reescritos para o novo visual: dia azul com ponto, dia branco sem texto, título do mês, aviso só para a equipe) e de cadastros (4), Testar 14/14, pacote Linux x64, smoke e segredos aprovados; captura de tela com dados simulados comparada à referência do PDF. Windows, macOS e ARM64 ficam com a matriz. Sem mudança no servidor, migration ou dependências.

## Fix 0.4.15 — lote 1 do Caça bugs 2 — publicada e verificada

A [0.4.15](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.15) foi publicada e verificada em 19/10/2026 pelo [run 39327380746](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39327380746), na primeira tentativa, após o PR [#17](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/pull/17). Tag/código testado `703be5998e2a4faaa12bc40c65c0a40b99b1b972` (merge em main). Conferidos os 12 assets e os 11 checksums contra digests públicos; v0.4.15 é a latest; as 20 releases anteriores mantêm arquivos, tamanhos e digests; links do README respondem. Relatório da CI: `Testes/teste22-2026-10-19_18-25-33-3f736ad4/log.txt`. O PR também trouxe a causa das falhas intermitentes de Maven (`Connection reset` ao baixar do Maven Central): `aplicativo/.mvn/maven.config` repete downloads (até 6 vezes) e a CI guarda as dependências em cache.

Lote 1 do PDF “Caça bugs 2”, autorizado em 19/10/2026. Mudanças:

- `GlobalModel`: `equipeLogada`, `secaoAtual` (início, agenda ou área interna, pela URL) e `solicitacoesPendentes` (`SolicitacaoPublicaRepository.existsByEstado("RECEBIDA")`, consultado só em `/admin`).
- `fragments.html`: o item ativo do menu principal vem de `secaoAtual`; “Solicitações recebidas” ganha `.pending-dot` com texto para leitores de tela.
- `public/agenda.html` + `agenda.js`: o aviso de cadastro de espaços só é renderizado para a equipe logada.
- `admin.js`: máscara de telefone (somente dígitos, até 11, `(48) 99808-2043`) em `input[type=tel]` e `input[name=telefone]`; números antigos com mais de 11 dígitos não são cortados ao abrir. O servidor já aceitava o formato.

Testes locais (Linux x64, 19/10/2026): 126 Java (um novo: destaque da Área Interna, bolinha de solicitações e aviso só para a equipe; um alterado: público não vê o aviso), Go test/vet, Node da agenda (9, um novo sem o aviso) e de cadastros (4, dois novos da máscara de telefone), segredos, Testar 14/14, pacote Linux x64 e smoke aprovados. Windows, macOS e ARM64 ficam com a matriz. Sem migration, reset, contas, dependências ou ativação externa. Lotes 2 a 5 aguardam decisão do responsável.

## Fix 0.4.14 — voltar da troca de senha e aviso de agenda sem espaços — publicada e verificada

A [0.4.14](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.14) foi publicada e verificada em 19/10/2026 pelo [run 39317656989](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39317656989). Tag/código testado `5cf993c5c34f7581a836229a08ab4f4511f1f097` (merge em main). Conferidos os 12 assets e os 11 checksums contra digests públicos; v0.4.14 é a latest; as 18 releases anteriores à 0.4.13 mantêm arquivos, tamanhos e digests; links de download do README respondem. Relatório da CI: `Testes/teste21-2026-10-19_17-14-33-8e2d6083/log.txt`. Caminho até a publicação: PR [#14](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/pull/14) (correções); duas execuções da main barradas por falhas intermitentes da CI (Maven no Ubuntu x64/Windows ARM; reinício e limpeza do servidor de teste no Windows x64), nenhuma reproduzida localmente; PR [#15](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/pull/15) passou a mostrar nome/mensagem de testes Java e linhas [ERROR] do Maven quando o job falha; PR [#16](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/pull/16) fez o `Testar.java` tentar de novo o início do servidor (até 3 vezes, só se ele encerrar ao iniciar) e a limpeza (até 10 s) no Windows. Pendência: a causa exata das falhas de Maven no Ubuntu x64/Windows ARM não foi identificada; se voltar, o diagnóstico do PR #15 mostra a mensagem.

Pedido em 19/10/2026 (PDF do responsável), com release e merge autorizados. O responsável escolheu manter o banco novo sem espaços (a equipe cadastra os reais) e só melhorar o aviso.

- `auth/password.html`: link **Voltar sem alterar** para `/admin`.
- `public/agenda.html`: aviso explica “Sem espaços” e oferece **Cadastrar espaço** (`/admin/cadastros/espacos/editar`) e a lista de espaços; CSS em `style.css`.
- Testes: `SecurityIntegrationTest.passwordPageOffersWayBackWithoutChanging`; `FluxosIntegrationTest.rotasPublicasEProtecaoCsrf` confere o botão.

Testes locais (Linux x64, 19/10/2026): 125 Java (dois testes alterados/novos: link de voltar na troca de senha e botão Cadastrar espaço na agenda), testes Go/vet/gofmt, Node da agenda e cadastros, segredos, Testar 14/14, pacote Linux x64 e smoke aprovados. Windows, macOS e ARM64 ficam com a matriz. Sem migration, reset, contas, dependências ou ativação externa. A PR #12 (macOS 12) foi descartada; o iniciador de macOS continua exigindo macOS 13.

## Fix 0.4.13 — agenda, validação e padronização — publicada e verificada

A [0.4.13](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.13) foi publicada e verificada em 19/10/2026 pelo [run 39243199166](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39243199166), após o PR [#11](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/pull/11) (matriz do PR no run 39241817701, verde). Tag/código testado `c3bdd011a08e80acb40143fadbe4633c7fa0080d` (merge em main). Conferidos os 12 assets e os 11 checksums contra digests públicos; v0.4.13 é a latest. As 18 releases/tags anteriores mantêm arquivos, tamanhos e digests. Relatório da CI: `Testes/teste17-2026-10-19_08-14-29-2e37f9bd/log.txt`.

Autorizado em 19/10/2026 após a revisão de qualidade. Mudanças:

- `static/js/agenda.js`: reescrito em formato legível, mesma lógica; o `catch` da consulta também limpa o detalhe do dia (horários, links e formulário de reserva), recuperando a correção da 0.4.8 que o retorno à 0.4.7 desfez. Regressão em `test_agenda_ui.cjs` falha no script anterior.
- `CrudService`: email de cadastros usa `ContactRules.EMAIL`; login usa `LoginNames.PATTERN`/`valid`, também em `PrivateAccountInitializer`. Teste `collaboratorEmailUsesTheSameFormatAsPublicContacts` falha com a regra anterior.
- Iniciador formatado com `gofmt`; `reserva.js` e `admin.js` formatados. O job `source` da CI recusa Go fora do `gofmt` e confere a sintaxe dos scripts da interface.

Validação: Testes locais (Linux x64, 19/10/2026): 124 Java (um novo de email), 48 Go (+race, vet nos três sistemas, gofmt limpo), 17 Node (uma regressão nova da agenda que falha no script anterior), segredos, Testar 14/14, pacote Linux x64 e smoke (15 logins, troca de iniciador, recuperação de órfão, recusa de host externo) aprovados. Windows, macOS e ARM64 ficam com a matriz. Testes Java ainda com estilo compacto não foram reformatados (exigiria formatador novo no Maven). Sem migration, reset, contas, preços, cotas, dependências ou ativação externa.

## Fix 0.4.12 — lote 2 desktop — publicada e verificada

A [0.4.12](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.12) foi publicada e verificada em 19/10/2026 pelo [run 39192696646](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39192696646), após o PR [#9](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/pull/9) (matriz do PR no run 39191819341, verde). Tag/código testado `f9e7ea1f831981d1dcb92637eac8808257806b18` (merge em main). Seis alvos Windows/macOS/Linux x64/ARM64, fonte/segredos, dependências, CodeQL, relatório e publicação aprovados. Conferidos os 12 assets e os 11 checksums contra digests públicos; v0.4.12 é a latest. As 17 releases/tags anteriores mantêm arquivos, tamanhos e digests. Relatório da CI: `Testes/teste15-2026-10-19_00-01-36-b40967e0/log.txt`.

Autorizado em 19/10/2026. Base: main da 0.4.11. Mudanças:

- `iniciador/update.go`: cliente sem prazo total; prazos de TLS/cabeçalho e `downloadStallTimeout` (60 s sem dados cancela pelo contexto); `progressReader` informa MB; consulta de releases em cache por 10 min; versão cuja instalação falhou não é tentada de novo na mesma execução; `removeStaleStages` limpa `.download-*`/`.stage-*`.
- `iniciador/main.go` + `alert_*.go`: `reportFatal` mostra aviso nativo quando `run()` falha, exceto com `--headless`.
- `security/LocalHostFilter.java`: perfil local aceita somente 127.0.0.1/localhost/::1 no Host.
- `reporting.go`: falhas de envio registradas uma vez por período. `model.go`: novas tentativas no Windows antes de remover o destino. `app.go`: `openBrowser` aguarda o auxiliar.
- `package_v04.py`: `--compress=zip-6`. `smoke_v04.py`: recusa de host externo.
- Documentação: guias sem versões antigas, índice, `continuidade/HISTORICO.md` com os estados anteriores, `CLAUDE.md` sem instruções vencidas. Resultado das suspeitas em REVISAO-DESKTOP.md.

Sem migration, reset, contas, preços, cotas, dependências ou ativação externa.

## Limpeza posterior à 0.4.12 (sem nova release)

Autorizada em 19/10/2026. Retirados: 14 rótulos de testes inexistentes e as verificações de `Backup.java` no `Testar.java`; dois testes Go redundantes; um método de repositório sem uso; ramos de código que nunca executavam em `ReservaService`/`CotaService` (os valores históricos do enum permanecem); agendamento da coleta de relatórios (pausado enquanto o envio dos PCs está desativado) e execução semanal da CI. Seções antigas deste handover e da validação estão em [historico/](historico/). Sem mudança de comportamento do executável; não há nova versão. Pendente de autorização: excluir os arquivos `aplicativo/scripts/Backup.java`, `config/DemoInitializer.java` e `docs/LEIA-PRIMEIRO.txt` e as cinco branches remotas antigas (a exclusão foi bloqueada pela proteção da sessão).

Entregas anteriores: [historico/HANDOVER-ENTREGAS.md](historico/HANDOVER-ENTREGAS.md).

## Sessões e relatórios introduzidos em 0.4.2

iniciador/session.go cria a sessão após adquirir o lock, grava JSONL incremental e metadados de versão em cada evento, mantém o ID durante handoff e recupera interrupções. iniciador/reporting.go consulta o destino público e retém os arquivos até confirmação final. SafeLogEncoder adiciona appVersion/launcherVersion/sessionId; SafeRoutes registra tipos de tela/ação sem parâmetros nem IDs na URI. Auditoria e autenticação mantêm códigos e IDs locais. O leitor do iniciador aceita somente campos permitidos da saída JSON do Java. PrivateLogDirectory aceita e protege as subpastas de sessões sem seguir links simbólicos; a regressão cobre as permissões e a preservação dos arquivos.

O receptor Node guarda fila temporária e não tem token de escrita do GitHub. O coletor autentica por OIDC com repositório, ID, main, workflow e audiência fixos. O GITHUB_TOKEN existe só no job e grava Testes/testeNN-data-id/log.txt e resumo.json. O índice recebidos.json mantém numeração global e deduplicação. Nunca executar conteúdo recebido como comando. A CI também publica sua versão e conclusão dos nove ambientes/gates antes da release.

A hospedagem requer confirmar o workspace no Render, publicar o receptor e ativar a URL real em Testes/configuracao.json. O arquivo público contém apenas enabled/endpoint, sem segredo. A ativação não exige reinstalar o executável. Sem receptor ativo, funcionam registros locais e relatórios de CI. Reinícios do servidor podem perder sua fila; a cópia local pendente permite reenvio na próxima abertura. Relatórios não atestam identidade do dispositivo.

Não houve migration, reset do banco nem troca de contas. Detalhes e limites: RELATORIOS.md, RECEPTOR.md e Testes/README.md. Validar sessões, versões, ausência de senhas, interrupção, fila offline, confirmação e handoff com o smoke do pacote. Testes Node cobrem receptor/coletor; o workflow confirma um token OIDC real. Não publicar logs operacionais brutos ou dados do perfil do usuário.

## Entrega

A 0.4 substitui a instalação por terminal/VS Code por pacotes nativos para Windows x64/ARM64, macOS Intel/Apple Silicon e Linux x64/ARM64. O iniciador Go abre uma interface local no navegador, instala o pacote completo no perfil do usuário, prepara chave/configuração, inicia/encerra o Java privado e consulta correções da linha 0.4.x. Não exige Java, Maven nem Go na máquina do usuário.

O aplicativo continua um monólito Spring Boot/Thymeleaf/JPA/H2, com Java 17/21, Flyway e os recursos da 0.3: agenda, reservas, CRM, permissões, logs, CSRF, sessões e backups. Migrations V1–V4 permanecem iguais. Apenas reservas aprovadas ocupam horários/consomem cota; conferência da identidade institucional e pagamentos continuam manuais. A 0.4 não declara atendimento integral ao PDF de melhorias funcionais.

A 0.4 usa banco novo no perfil do usuário, separado da pasta da 0.3. Não apaga nem importa automaticamente dados anteriores. Preserva estrutura/referências e começa sem cadastros ou reservas de exemplo. Cinco contas públicas são criadas no banco, com BCrypt, e restauradas ao iniciar; consulte ACESSOS.md. Não há arquivo de senhas obrigatório. Esse modo serve à demonstração local, não a dados confidenciais. Produção permanece um procedimento separado e não é implantada por esse iniciador.

## Código e operação

- `iniciador/`: iniciador Go sem dependências externas; interface embutida, atualização, controle do processo, lock por instalação e teste de pacotes.
- `config/LocalAccounts.java`: cinco contas públicas de demonstração. `PrivateAccountInitializer` usa-as no perfil local e mantém o provisionamento privado para manutenção/produção.
- `config/LocalLauncherLifecycle.java`: recebe pedido de encerramento por arquivo no diretório privado; fecha o contexto antes de finalizar a JVM. Não cria rota HTTP de desligamento.
- `InpetuApplication`: lock da instalação e modo offline `--verificar-parado`, usado antes de iniciar/atualizar.
- `.github/scripts/package_v04.py`: compila o iniciador, gera runtime com jlink, inclui JAR/manifesto e monta os pacotes.
- `.github/scripts/smoke_v04.py`: executa o binário real em dados descartáveis e valida instalação, cinco logins, perfis, cadastro, reinício e encerramento.
- `.github/scripts/publish_v04.py`: confere os seis pacotes/artefatos do mesmo commit e publica somente após os gates de CI.

O controlador escuta somente 127.0.0.1 em porta dinâmica; exige token aleatório, origem e Host válidos nas ações. O token fica no estado privado do usuário, não em logs nem no repositório. O backend também fica restrito ao próprio computador. O lock impede abrir duas instâncias do iniciador na mesma instalação. Não iniciar manualmente dois processos Java sobre o mesmo banco.

Dados: `InPETU-Hub/0.4` dentro de LocalAppData (Windows), Application Support (macOS) ou configuração do usuário (Linux). Contém `data/inpetu-v02.mv.db`, `.env.local`, `.secrets/mfa.key`, logs, versões instaladas e backups. O nome histórico da chave é mantido; ela participa de outras verificações mesmo sem MFA. Preserve banco, chave e ambiente em conjunto. Fechar só a aba mantém o iniciador; use os botões Parar/Fechar para encerrar.

## Atualização e retorno

Somente releases estáveis 0.4.x são elegíveis. Os ZIPs incluem runtime e aplicativo, são conferidos por SHA-256 e extraídos em uma pasta nova com verificação de caminhos. Nunca sobrescrever um executável em uso. Configuração e banco ficam fora dos pacotes; uma correção não redefine registros de negócio. Alterações nas cinco contas de demonstração voltam ao padrão no próximo início, por decisão desta versão.

Antes da troca, é guardado um snapshot privado dos dados/configuração. Não é cifrado e não substitui um plano de backup. Não há rollback automático de migrations após falhas: pare, preserve o estado atual e restaure backup completo numa cópia da versão anterior. Uma restauração perde alterações posteriores. Não compartilhar backups, chaves ou logs sem revisão.

## Histórico de validação e limites

A entrega 0.4.3 passou nos seis runners e foi publicada pelo [run 39020936011](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39020936011), commit b06fcf9d89161a6c512d4e4f3c97c6c28f20ac54. Foram conferidos os 12 arquivos publicados e a preservação das sete releases anteriores. O comando Java do roteiro tem o argumento de codificação entre aspas para funcionar também no PowerShell. A publicação exige testes Go, testes Java e o roteiro completo de regras/HTTP/backup, empacotamento e smoke do executável em seis runners nativos. Fonte, segredos, dependências e CodeQL são gates separados. O resultado de cada entrega fica em Actions; um teste local isolado não substitui a matriz.

Os testes verificam os binários e a interface HTTP; não substituem teste visual com pessoas nem comprovam todas as versões de Windows, macOS e distribuições Linux. Os executáveis não têm assinatura comercial/notarização; o sistema pode pedir confirmação, e políticas corporativas podem bloquear. Um navegador padrão e ambiente desktop são necessários à interface. Não foi realizada implantação MySQL/SMTP/TLS em produção. Mantém-se a limitação conhecida de resultados CodeQL: ausência de alertas não significa ausência de falhas nem garante execução completa de todas as consultas.

## Próximas entregas

Atualizar VERSION, POM, caminhos de JAR do roteiro de testes e as ferramentas de publicação e RELEASE-NOTES. O build passa a versão ao Go. Preservar o protocolo de `bundle.json`, nomes dos assets, armazenamento da linha 0.4 e compatibilidade de migrations. Corrigir vulnerabilidades de runtime também exige gerar e publicar novo pacote; Java privado não é atualizado pelo gerenciador do sistema. As linhas antigas permanecem nas tags e releases; seus workflows não fazem parte da árvore principal atual. Use a release/commit recebido como referência para manutenção.

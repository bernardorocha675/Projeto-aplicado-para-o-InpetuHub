# 0.4.9 publicada — aplicativo e área interna restaurados

O responsável aprovou o visual da agenda e pediu a restauração da área interna. Corrigida a regressão da 0.4.8: /admin havia ficado escondido no rodapé, logo/Início apontavam para o site externo e a gestão herdava navegação/CSS institucionais. Área interna e Solicitar reserva agora estão no topo; logo/Início são locais. Admin/auth usam seus próprios fragmentos e preservam painel, reservas, solicitações, cadastros, administração e sidebar. O visual público aprovado permanece. Detalhes em docs/RESTAURACAO-AREA-INTERNA.md.

A [0.4.9](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.9) foi publicada e verificada em 18/10/2026 pelo [run 39159176375](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39159176375), com os 11 jobs obrigatórios aprovados. Tag/código testado `d0aeb5f364145f99b4804ff2ce8b6578b9f2313c`. Os seis alvos Windows/macOS/Linux x64/ARM64 executaram testes, roteiro HTTP/backup, empacotamento e smoke do runtime privado, incluindo os acessos restaurados. Conferidos os 12 assets, os 11 checksums contra digests públicos e o ZIP de código baixado. As 14 releases/tags anteriores mantêm links, arquivos e digests, inclusive Android experimental. Relatório de CI: `Testes/teste12-2026-10-18_16-31-49-b1b838ba/log.txt`.

Passaram 120 testes Java (dois novos), 33 Go com detector de concorrência/go vet, 19 Node, roteiro HTTP/permissões/persistência/backup, check local de segredos e smoke Linux x64. Chromium 154 verificou login, painel e menus, cotas/auditoria/senha, logout e reserva em larguras de 390 a 1919px, sem erros JavaScript. Fonte/segredos, dependências, CodeQL, relatório e publicação passaram na nova matriz. A CI comprova os seis runners previstos, sem afirmar teste em todas as distribuições ou navegadores.

Sem reset, migrations, alteração de contas/senhas, permissões, reservas, preços, cotas ou dependências. Android continua retirado; SMTP, Render e envio externo permanecem desativados. A conferência externa da marca/fontes/submenus/links oficiais continua pendente pelo bloqueio HTTP 403 já documentado; não impede a navegação local. O ZIP de código preserva o snapshot anterior ao relatório e fechamento documental; não substituir assets nem mover tags. Entrega concluída: aguardar nova autorização para outro lote.

---

# 0.4.8 publicada — PRs #5/#6/#7 integrados, entrega concluída

A [0.4.8](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.8) foi publicada e verificada em 18/10/2026, exclusivamente pelo [run 39157060612](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39157060612), com os 11 jobs obrigatórios aprovados. Tag/código testado `2de098c6099af08d1fd44963978ccef94ce14adc`. Os seis alvos nativos executaram testes, roteiro HTTP/backup, empacotamento e smoke com Java privado. Conferidos os 12 assets e os 11 checksums contra digests públicos; ZIP de código baixado e verificado, incluindo quatro fontes/licenças e os SHAs oficiais das três ações. As 13 releases/tags anteriores mantêm links, arquivos e digests; nenhum asset histórico foi substituído. Relatório de CI: `Testes/teste11-2026-10-18_16-04-21-0ced77a7/log.txt`.

O responsável autorizou verificar os três PRs e publicar tudo após informado sobre os limites da preparação. Os PRs aparecem como merged no GitHub; os commits do Dependabot foram preservados. Atualizações restritas a download-artifact 8.0.2, setup-go 7.0.0 e setup-java 6.1.0. JDK 21, Go 1.27.2, seis alvos, gates e regras preservados. As etapas HTTP/backup dos dois Windows que falharam no PR #7 histórico passaram no novo run, incluindo smoke; causa histórica não confirmada porque os logs antigos retornam 404. Não confundir falha antiga com o resultado aprovado desta integração.

A interface da agenda foi entregue pelas capturas, com fontes locais e fluxo de reserva preservado. Continua pendente conferir o arquivo da marca, fontes/CSS originais, destinos dos links, submenus e redes sociais: o domínio oficial ainda está bloqueado por HTTP 403. Publicação autorizada com os limites descritos; não declarar cópia integral idêntica nem sincronização de eventos da UFSC. Detalhes em docs/IDENTIDADE-INSTITUCIONAL.md e docs/REVISAO-PRS-2026-10-18.md.

Passaram os 118 Java, 33 Go/-race/vet, 19 Node, roteiro HTTP/backup, Chromium e smoke locais da preparação; as ações integradas passaram na nova matriz completa, sem enfraquecer testes. Fonte/segredos/dependências/CodeQL e relatório/publicação aprovados. Testes/configuracao.json permanece desativado. Sem reset, migrations, mudança de contas/senhas/preços/cotas, Android, SMTP, Render ou envio externo. Chaves/bancos/backups fora dos pacotes e do Git.

O ZIP de código preserva o snapshot do commit testado antes do relatório da CI. O fechamento acompanha main sem substituir assets publicados. A configuração cloud permanece salva como rascunho; sua aplicação/publicação pelo produto não foi declarada concluída. **Entrega publicada e verificada. Parar e aguardar autorização para outro lote.**

---

## Registro anterior: autorização e execução da publicação

# Publicação 0.4.8 autorizada — PRs integrados, aguardando CI de main

Em 18/10/2026 o responsável mostrou os PRs #5/#6/#7 e disse “Verifica isso e pode publicar tudo”, após informado sobre a preparação da interface e o acesso oficial bloqueado. Essa nova autorização substitui a retenção anterior da publicação pela conferência visual. Não declara equivalência integral da marca/fontes/links/submenus: essa pendência continua documentada.

Base da retomada limpa: `90c997827fbe979d32e066e675eb916c9f1e5c3d`, branch work; main remoto `215e57d2055faeb4a6b0455436a71a9f26a9115d`. Lidos CLAUDE, continuidade, handover e RELEASES; AGENTS ausente. Revisados e integrados os três PRs sem conflitos, preservando os commits originais: download-artifact 8.0.2, setup-go 7.0.0 e setup-java 6.1.0 com SHAs das tags oficiais. Somente as referências de ações do workflow mudaram. JDK 21, Go 1.27.2, seis alvos, qualidade, segredos e publicação por gates mantidos.

PRs #5/#6 têm matrizes históricas aprovadas. PR #7 tinha falhas nos dois alvos Windows, em Testar.java/HTTP/backup, depois do setup Java; os logs antigos retornam 404, sem causa histórica confirmada. Validar a integração atual em nova matriz completa, sem mascarar testes. YAML/contrato do workflow, 19 Node e segredos passaram localmente. Java/Go/UI não mudaram em relação à preparação, cuja evidência aparece abaixo.

A versão continua 0.4.8, ainda sem tag ou release no início da retomada. Baseline das 13 releases/tags anteriores registrada antes de publicar. Preparar documentação e enviar main; o workflow deve executar os seis pacotes nativos, fonte/segredos/dependências/CodeQL, relatório de CI e publicação dos artefatos do mesmo run. Só declarar publicado após conferir tag, SHA, arquivos, checksums e preservação do histórico. Sem reset, migrations, mudanças de contas/preços/cotas, Android, SMTP, Render ou envio externo. Detalhes em docs/REVISAO-PRS-2026-10-18.md.

---

## Registro anterior: preparação retida antes da nova autorização

# Identidade institucional — 0.4.8 em preparação, publicação bloqueada

Pedido do responsável em 18/10/2026: aparência igual ao site original, porque o projeto aplicado é oficial do InPETU Hub. URL fornecida: https://inpetuhub.sites.ufsc.br/agenda/; três capturas de referência. Base limpa `215e57d2055faeb4a6b0455436a71a9f26a9115d`, branch `work`; remoto main no mesmo SHA. AGENTS.md ausente; CLAUDE, continuidade, handover e RELEASES.md lidos. A release pública atual permanece [0.4.7](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.7), cujo run obrigatório passou nos 11 jobs.

Implementados pelas capturas: cabeçalho/rodapé compartilhados, textos, painel e calendário com proporções institucionais, fontes locais OFL, marcação somente de eventos reais, seleção e detalhes. Filtros, capacidades e reserva preservados sob Consultar espaços e solicitar reserva. Não há sincronização/importação da agenda externa nem registros de exemplo incluídos no banco. Código identifica fontes como ASSET e permite WOFF2 aos visitantes. Corrigido catch que deixava links de reserva da consulta antiga após erro de atualização; regressor falha sem a limpeza e passa com o patch.

Passaram localmente 118 Java, 33 Go/detector de concorrência/go vet e 19 Node; roteiro HTTP/perfis/persistência/backup/restauração aprovado em 18:41:01 UTC. JAR final recompilado e Chromium 154 com fluxo completo, zero erros JS e larguras 390/768/1024/1919px. Visual simulado usa exclusivamente dados sintéticos no navegador; fluxo de reserva usa endpoint real, em instalação descartável separada. Detalhes em docs/VALIDACAO.md e docs/IDENTIDADE-INSTITUCIONAL.md. Pacote final Linux x64 com Java privado e smoke aprovados: 15 logins, reinício, troca do iniciador e banco/chave preservados. Código testado `12bdb6b169b4020692deb88584751ac80757203a`, enviado para `release-v0.4`. [Run 39155638958](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39155638958): nove jobs aprovados — Ubuntu 24.04 x64/ARM64, macOS 15 Apple Silicon/Intel, Windows Server 2025 x64, Windows 11 ARM64, fonte/segredos, dependências e CodeQL. Cada alvo nativo executou testes, roteiro HTTP/backup, empacotamento e smoke. Relatório público em Testes e publicação de release não foram executados: são restritos a main. Não confundir estes nove checks com uma release publicada ou com o pipeline de publicação completo.

**Bloqueio concreto:** domínio oficial recusado pela rede com HTTP 403. Rascunho aditivo para inpetuhub.sites.ufsc.br salvo; ainda é necessário revisar/salvar nas configurações e publicar o ambiente. Sem acesso, faltam conferir marca/fontes originais, destinos provisórios do menu/legal, submenus e links sociais. A preparação não comprova cópia integral idêntica e não deve ir a main/publicação antes dessa conferência. A preferência opcional sobre copiar páginas institucionais completas não foi respondida; escopo assumido é agenda e identidade compartilhada.

Conferidas novamente as 12 releases históricas e a 0.4.7: links/digests dos assets e todas as 13 tags preservados. Main permanece em `215e57d2055faeb4a6b0455436a71a9f26a9115d`. Versões de código preparadas para 0.4.8 na família 0.4.x. Publicação/tag/release ainda não realizadas. A branch release-v0.4 pode executar a matriz sem publicar; não usar o publicador manual nem substituir assets anteriores. Sem reset, migrations, mudança de contas/senhas/preços/cotas, Android, SMTP, Render ou envio externo. A configuração cloud teve install_script/start_skill atualizados e salvos: usa a VERSION do checkout e dados de desenvolvimento fora do repositório, com bloqueio de nova chave se já há dados/configuração. Testados os quatro cenários de chave e a inicialização real isolada com agenda/login/fontes HTTP 200. O rascunho ainda precisa ser revisado/salvo e o ambiente publicado; isso não foi declarado aplicado ou restaurado numa nova tarefa. Depois de terminar esta identidade e a release, parar e aguardar novo lote.

---

# Relatório caçabugs — 0.4.7 publicada e concluída

Em 18/10/2026 o responsável enviou `caçabugs_0.1_Joaoe.docx` e confirmou corrigir **todos os itens restantes** nesta entrega. Foram lidos texto e 12 capturas; original não foi incluído no Git. Base `efe162a1ff9b1296a22b8ab4fff61174ca70af19`, checkout inicialmente limpo e remoto main no mesmo SHA. A entrega atual é a [0.4.7](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.7). Código testado e tag: `992168e3ceb39f196b2153aa5e291fbae0e2f196`. [Run 39152880226](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39152880226): 11 jobs aprovados, incluindo os seis alvos nativos e fonte/segredos/dependências/CodeQL/relatório/publicação. Conferidos 12 assets e SHA256SUMS contra digests públicos; preservadas as 12 tags/releases anteriores com os mesmos arquivos e digests. Relatório da CI incorporado em `04a3d34`.

Implementados: validação de contatos no navegador/servidor/cadastro, rollback/reenvio de edição inválida; data ISO em pt-BR e rascunho da sessão para editar o resumo; janela de hoje até 31/12 do próximo ano com avanço anual; confirmação da sessão sem campo de código inexistente no modo local; consulta da agenda por dia com capacidades, indicação livre/parcial/ocupado, sete colunas em largura estreita e asteriscos nos obrigatórios. A correção de capacidade 0.4.4 e dos erros/logs 0.4.5 foi preservada e revalidada. Causas/evidências em docs/CORRECOES-RELATORIO.md; mapa em docs/BUGS.md.

Contatos novos usam grupo de validação de entrada, preservando o processamento/persistência de solicitações históricas. Regressão comprova recusa de registro legado sem reescrever seu contato. Sem migration, reset, mudança de contas/senhas, preços/cotas, dependências ou ativação externa. A janela de datas foi alterada por pedido explícito do relatório; preços futuros ainda exigem tabela cadastrada. Confirmação obrigatória de produção, expiração e tentativas permanecem intactas. Android continua retirado.

Validação local: 117 Java (18 novos), 33 Go/detector de concorrência/go vet, 16 Node; roteiro HTTP/backup e Chromium 154 em pt-BR com desktop/390px, zero erros JS. Fluxo agenda → reserva → erro/correção → revisão/edição → conferência manual verificado no JAR final. O primeiro conjunto de 13 regressores apresentou 12 falhas antes do patch. Smoke final Linux x64 aprovado: 15 logins, permissões, agenda, cadastro, reinício, troca real do iniciador e banco/chave preservados. A matriz remota executou com sucesso os seis pacotes Windows/macOS/Linux x64/ARM64, incluindo roteiro HTTP/backup e smoke. Isso não comprova todos os navegadores, dispositivos ou distribuições.

A 0.4.7 foi publicada exclusivamente pelo workflow após os gates obrigatórios. O ZIP de código contém o snapshot anterior à CI; este fechamento acompanha main sem substituir assets. **Todos os itens restantes do anexo foram atendidos neste lote. Parar e aguardar nova autorização.** SMTP real não foi testado/ativado; Render e envio externo permanecem desativados.

---

# Primeiro lote desktop 0.4.6 — publicado e concluído

Pedido do responsável em 18/10/2026: retirar Android do produto ativo, revisar o desktop e entregar um primeiro lote pequeno, parando depois. Base `64487c7e17e254a5c743e16d375ce3eb2020ee9a`, branch local `work`, inicialmente limpa e remoto main no mesmo SHA. Instruções, continuidade, handovers e RELEASES.md lidos; não havia AGENTS.md no workspace.

[Release 0.4.6](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.6) publicada e verificada. Código testado e tag: `5d6e2df8b3d46da9041df97df9504627a0e891cb`. [Run 39150431777](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39150431777): 11 jobs aprovados, incluindo seis ambientes nativos, fonte/segredos/dependências/CodeQL, relatório e publicação. Confirmados os 12 assets, SHA256SUMS contra digests públicos e preservação das 11 tags/releases anteriores com os mesmos arquivos e digests, inclusive a experimental Android. Relatório da CI incorporado em `2a03ed6`. O ZIP de código é o snapshot do commit testado; este fechamento acompanha main sem substituir assets.

Android retirado da árvore atual: fontes/recursos/testes/scripts, workflow, certificado público e três guias exclusivos. Não havia APK rastreado, chave privada ou backup entre os arquivos retirados. Suporte retirado por decisão do responsável; não retomar sem nova autorização. Histórico e materiais privados de recuperação preservados.

Duas falhas reproduzidas e corrigidas: configuração ausente com dados existentes impede geração de outra chave/ambiente e orienta restaurar backup completo; interrupção de leitura do download mantém a versão íntegra instalada utilizável. Erro de disco e integridade continuam bloqueando troca. Arquivos, impacto, evidência e suspeitas separados em docs/REVISAO-DESKTOP.md.

Passaram 99 Java, 33 Go/go vet/detector de concorrência local, 14 Node, roteiro HTTP/backup/contas e smoke do executável Linux x64 com Java privado, 15 logins, reinício, atualização simulada e banco/chave preservados. Compilação cruzada local dos outros cinco alvos aprovada; testes nativos executados pelos seis runners. Links locais/YAML e check de segredos aprovados. O check local OSV foi bloqueado pela rede, mas seu gate remoto passou. API GitHub/gh locais indisponíveis não impediram o envio por Git HTTPS e a verificação pública da CI/release.

Sem reset, migrations, mudança de regras/contas/dependências, hospedagem ou envio externo. Testes/configuracao.json permanece desativado. Não declarar compatibilidade além dos runners testados nem ausência geral de vulnerabilidades. Limites em docs/VALIDACAO.md.

**Lote concluído. Parar e aguardar autorização para o próximo.** Suspeitas sobre ambiente do check de lock, substituição atômica Windows e processo do navegador permanecem para outra etapa. Não ativar Render ou avançar automaticamente itens antigos do PDF.

---

# Etapa entregue — logs 0.4.5; ativação do receptor pendente

Pedido de 17/10/2026: criar novo fix terminando a questão dos logs. Base c27ad98da17796b92ad1d05819b05bf0218c9f1a, release anterior 0.4.4. Esta etapa prioriza logs e não autoriza automaticamente outras partes do PDF.

[Release 0.4.5](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.5) publicada e verificada. Tag e código testado: `1fac5b0bf5f432db48f85f63181c037859817f23`. [Run 39028486594](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39028486594): 11 jobs aprovados, incluindo seis ambientes. Foram conferidos os 12 arquivos e a preservação das nove releases anteriores. HEAD após o relatório da CI, antes deste fechamento: `528dae27959475511ebc363b5e0c9ab6ecb34ed6`.

Entregue: eventos para erros tratados/inesperados/validação/SMTP, respostas HTTP adequadas e referência de suporte, recuperação de finalização incompleta, preservação de conteúdo em reparo legado e acompanhamento claro de recibos pendentes. Passaram 99 testes Java, 30 Go e go vet, cinco Node da agenda, dois da interface administrativa e sete do receptor/coletor; roteiro HTTP/perfis/contas/backup e smoke com erro controlado nos seis executáveis. Evidências e limites: docs/VALIDACAO.md. O ZIP de código contém o instantâneo anterior à CI; este registro final acompanha main sem alterar assets publicados.

O Render foi consultado: nenhum workspace selecionado. A integração exige perguntar ao usuário qual workspace usar; My Workspace foi listado, mas não selecionado. Testes/configuracao.json continua enabled=false/endpoint vazio. Não criar recurso nem ativar destino sem essa confirmação explícita. O fix está publicado; solicitar a confirmação concreta de My Workspace para um único receptor Node gratuito, com auto-deploy desativado. Os testes de transporte são simulados, não uma implantação real. Não declarar envio de PCs como concluído.

Sem reset, migrations, mudança de contas/senhas ou dados. Referências históricas abaixo registram 0.4.4; o estado inicial desta seção prevalece para a tarefa atual. A entrega de código está concluída; aguardar autorização/confirmação; não continuar outro item do PDF.

---

# Estado atual e ponto de retomada

Registro de 17/10/2026. Repositório: bernardorocha675/Projeto-aplicado-para-o-InpetuHub, branch main. Confira o HEAD, Actions e Releases ao retomar.

## Tarefa autorizada

O usuário enviou caçabugs_0.1_Joaoe.docx.PDF e pediu analisar e corrigir por partes, concluir uma parte e publicar novo fix antes de parar. O PDF completo e suas imagens foram lidos. A divisão dos itens está em docs/BUGS.md. A primeira parte é somente o cadastro de espaços; as partes seguintes dependem de nova autorização.

## Entrega publicada: 0.4.4

Base: a5cfdd9f603674baf7ea67991a4ecb3edecb604f, após v0.4.3. A [v0.4.4](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.4) foi publicada e verificada. Código testado e tag: `fbe03611991e1ce2678eab93615594e3e90223b1`. [Run 39025880429](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39025880429): 11 jobs aprovados, incluindo os seis ambientes nativos. Os 12 arquivos foram conferidos; as oito releases anteriores mantêm seus IDs e arquivos/tamanhos/digests. HEAD após relatório da CI, antes deste fechamento documental: `6829e3e7a89d619479244149e5245c6607f3ae53`.

- Capacidade de espaços inteira positiva no navegador e no servidor; zero, negativos, frações e estouro são rejeitados.
- Erro de regra retorna HTTP 400 no mesmo formulário, com nome, capacidade e checkboxes preservados. A transação inválida é revertida antes da resposta.
- Corrigir e reenviar permite criar/editar; ao voltar pelo cache, o botão é reabilitado. A proteção contra cliques repetidos durante envio continua.
- Rejeições do servidor registram CADASTRO_INVALIDO/NEGADO sem dados livres. Tentativas bloqueadas só no navegador não chegam ao servidor.
- Novos testes de integração usam banco descartável separado e transações reais por requisição; testes Node executam o JavaScript real.

Sem reset, migration, mudança de senhas/contas/permissões ou diretório de dados. Demais formulários e valores financeiros mantêm as regras anteriores. Handovers e versões dos componentes/pacotes foram atualizados.

Validação efetiva da 0.4.4: 91 testes Java (12 novos), 27 Go e go vet, cinco Node da agenda, dois da interface administrativa e sete do receptor/coletor; roteiro HTTP/perfis/contas/backup e smoke do executável real. Passaram os seis ambientes com JDK 21, além de fonte/segredos/dependências/CodeQL/relatório/publicação. Testes locais também passaram com JDK 17. Detalhes: docs/VALIDACAO.md. O ZIP de código contém o instantâneo anterior à CI; este fechamento acompanha main sem substituir os assets imutáveis.

**Parte 1 concluída. Parar aqui e aguardar nova autorização antes da parte 2 ou de qualquer outra etapa.**

## Próximas partes — parar antes de iniciar

1. Validar nome/email/telefone nos fluxos de reserva e cadastro, com mensagens compreensíveis.
2. Datas: limitar ao ano atual/próximo, preservar seleção, investigar preenchimento agenda → reserva e remover mensagens técnicas. A captura de conversão no PDF pertence à reserva, apesar de o texto dizer login; não comprova falha de autenticação.
3. Confirmação: hoje o código só existe com email configurado. O modo local permite conferência manual, mas a tela ainda oferece código. Rever a experiência sem inventar código nem enfraquecer produção.
4. Agenda: espaços/capacidades por dia, visual livre/ocupado e indicação de obrigatoriedade.

O acompanhamento completo está em docs/BUGS.md. Não declarar que todos os itens do PDF foram corrigidos.

## Base e operação preservadas

Fontes atuais: aplicativo/ (Spring Boot Java), iniciador/ (Go), receptor/ (Node); guias em docs/. O usuário usa exclusivamente o executável. Não recriar versões históricas em pastas nem instruções de uso por VS Code. Versões antigas permanecem nas tags/releases; não mover tags ou substituir assets publicados. Backup ZIP anterior à reorganização foi entregue e verificado com fontes, histórico, sete branches e sete tags; binários antigos ficam nas releases.

As instalações usam InPETU-Hub/0.4 no perfil do usuário: data/inpetu-v02.mv.db, .env.local, .secrets/mfa.key, versões, logs e backups. Preservar o conjunto. Cinco acessos públicos são de demonstração local e restaurados no início por decisão anterior; não copiar credenciais para a continuidade nem usar dados confidenciais na demonstração. Produção permanece protegida e não foi implantada.

Sessões e logs começam automaticamente ao abrir o iniciador e incluem versões. A sessão continua durante troca de executável e é recuperada após interrupção. Fechar só a aba não finaliza; usar Fechar iniciador. CONCLUIDO indica encerramento normal, não ausência de erros. Eventos/tamanho são limitados. Somente relatórios sanitizados podem ser publicados.

A matriz exige Ubuntu 24.04 x64/ARM64, macOS 15 Intel/Apple Silicon, Windows Server 2025 x64 e Windows 11 ARM64; JDK 21, Go 1.27.2, Maven 3.9.9 com checksum. O alvo Java continua 17. Manter aspas em `java "-Dfile.encoding=UTF-8" --source 17 scripts/Testar.java` para o PowerShell. Testes locais não comprovam todas as edições/navegadores. Mantém-se a limitação conhecida de interpretação de consulta CSRF no CodeQL; não afirmar ausência de vulnerabilidades.

## Pendente: envio dos computadores do time

Testes/configuracao.json permanece enabled=false e endpoint vazio. Logs locais e relatórios de CI funcionam; envio de PCs ao GitHub ainda não foi ativado. Não usar relatórios de CI como prova de envio dos PCs.

O receptor Node não possui credencial GitHub e mantém fila temporária. O coletor usa OIDC restrito ao workflow/main/repositório e GITHUB_TOKEN dentro de Actions. HTTP 202 não é confirmação final; cópias locais permanecem até recibo de publicação e permitem reenvio na próxima abertura. O servidor pode perder a fila ao reiniciar; plano gratuito pode suspender. Não prometer entrega com o computador fechado e fila perdida.

Render estava conectado e My Workspace foi observado, mas a integração exigiu confirmação explícita de workspace antes de criar o serviço. Essa confirmação ainda não foi respondida especificamente. Não criar serviço, ativar endpoint ou interpretar este fix como autorização de hospedagem. Conferir estado e serviços reais quando houver autorização. Procedimento: continuidade/OPERACAO.md e docs/RECEPTOR.md.

O responsável operacional, aceite do parceiro e processo de produção ainda precisam ser definidos. As conexões do ChatGPT não passam automaticamente ao Claude Code. Nunca gravar tokens, banco, chaves, backups ou logs operacionais brutos nesta área ou em Testes/.

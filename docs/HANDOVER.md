# Handover técnico — InPETU Hub 0.4.7

Base desta reorganização: `1d7f8b0ff9bb97d1cf74fef1e195b01f674d4a0a` (main após 0.4.2 e continuidade). A release 0.4.2 permanece no commit `c9a63fd60ff8583edcceb30287eefc1ca494209f`. Base da linha 0.4: `12abbbcf9956ea68804229ac37ea527fb181f9df` (0.3.3). Entrega acadêmica de Projeto Aplicado do UniSENAI com o InPETU Hub. Equipe: Bernardo Rocha, João Mussiol, João Feitosa e Guilherme Batista. O responsável operacional e o aceite do parceiro ainda precisam ser definidos.

## Correções do relatório — 0.4.7

O responsável autorizou todos os itens restantes do DOCX em 18/10/2026. ContactRules compartilha formatos entre entrada pública e cadastro; o DTO usa grupo de validação de entrada para preservar a persistência de registros históricos. Cadastro inválido reapresenta campos após rollback, incluindo edições. DateTimeFormat ISO corrige valores incompatíveis com input date em português; o resumo retorna ao rascunho da sessão sem depender de history.back.

ReservationDates fornece fim do ano seguinte no Clock/fuso da aplicação; DisponibilidadeService valida a janela também no servidor. Não são alterados preços/cotas; o gestor deve cadastrar tabela do ano futuro. A tela de confirmação deriva o estado da solicitação da sessão e só pede código ativo; modo manual não altera os requisitos de produção. A agenda inclui seletor de dia, capacidades, visual de disponibilidade e ajuste para sete colunas em largura estreita. Obrigatórios indicados por asteriscos.

Detalhes e regressões: [CORRECOES-RELATORIO.md](CORRECOES-RELATORIO.md). A versão de trabalho 0.4.7 aguarda validação final/publicação obrigatória. Sem migrations, reset, mudança de contas/dependências ou ativação externa. Android continua retirado.

## Primeiro lote 0.4.6 — foco desktop e confiabilidade

Por decisão do responsável em 18/10/2026, Android saiu do produto ativo. Fontes, testes, workflow e guias exclusivos foram retirados; histórico, prerelease e materiais privados de recuperação permanecem preservados. Desktop continua Windows/macOS/Linux x64/ARM64, Java privado e dados na família 0.4.

O iniciador verifica a configuração antes de gerar arquivos: se há dados e falta chave ou ambiente, interrompe e orienta recuperar o conjunto original. Instalação nova continua automática. Download interrompido mantém a versão íntegra instalada utilizável; integridade e erros de disco continuam bloqueando a troca. Duas regressões reproduziram as falhas antes das correções. Detalhes e suspeitas em [REVISAO-DESKTOP.md](REVISAO-DESKTOP.md).

A [0.4.6](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.6) foi publicada pelo [run 39150431777](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39150431777), com os 11 jobs aprovados, incluindo os seis ambientes nativos. Tag no commit `5d6e2df8b3d46da9041df97df9504627a0e891cb`; 12 assets conferidos, SHA256SUMS coerente com os digests públicos. As 11 tags/releases anteriores mantêm seus arquivos e digests, inclusive Android. Relatório da CI incorporado em `2a03ed6`. Este fechamento documental acompanha main, sem substituir o código ZIP anterior à CI.

Passaram 99 testes Java, 33 Go/go vet/detector de concorrência local, 14 Node, roteiro HTTP/backup e smoke do executável; compilação cruzada local para os outros cinco alvos e execução nativa pela matriz. Sem reset, migration, mudança de contas/regras/dependências ou ativação externa. Lote concluído: parar e aguardar nova autorização.

## Fix 0.4.5 — logs e finalização de relatórios

Base: c27ad98da17796b92ad1d05819b05bf0218c9f1a, após 0.4.4. O pedido atual é um fix dedicado aos logs; não autoriza continuar automaticamente as demais partes do PDF.

GlobalExceptionHandler registra exceções tratadas com códigos, rota normalizada, tipo/pontos da exceção e requestId, sem mensagem livre. Regras retornam 400, conflitos de integridade 409 e falhas inesperadas 500; ErrorResponse do Spring preserva o código original, e exceções de autenticação/autorização seguem com o Spring Security. A tela de erro mostra referência de suporte; o evento HTTP e o diagnóstico compartilham o identificador. A validação capturada em PublicController gera VALIDATION_REJECTED/HTTP 400; o formulário mostra orientação simples em vez do texto de conversão do framework. Não foram alteradas regras de nome, telefone ou janela de datas nesta etapa.

EmailService emite EMAIL_FAILED com fase/tipo/pontos, sem destinatário, código ou credenciais, e distingue envio concluído/configuração ausente. Não há SMTP real de produção ativado.

No Go, finish grava relatorio.json/log.txt antes de confirmar o estado terminal em sessao.json, e verifica erros de sync/fechamento. A recuperação refaz sessões incompletas; para registros antigos já terminados, preserva resultado, horário final e eventos para manter o mesmo conteúdo. Envio exige metadados e log final coerentes antes de postar. A interface informa quando existem relatórios aguardando confirmação; falhas de configuração/envio geram eventos sanitizados. O esquema público de relatórios permanece 1, sem campos livres novos.

Novos testes: erros MVC/segurança, falha de SMTP simulada, finalização com erro de escrita, reparo antigo sem alteração do conteúdo, diagnóstico de envio e erro controlado no smoke real. A [v0.4.5](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.5) foi publicada com tag `1fac5b0bf5f432db48f85f63181c037859817f23` pelo [run 39028486594](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39028486594): 11 jobs aprovados, incluindo seis ambientes nativos. Foram conferidos os 12 arquivos e a preservação das nove releases anteriores. Passaram 99 testes Java, 30 Go/go vet, cinco Node da agenda, dois da interface administrativa e sete do receptor/coletor, além do roteiro HTTP/backup e do smoke com erro controlado no executável. Consulte VALIDACAO.md para limites e evidências.

O envio real continua desativado. Nesta retomada, a integração Render retornou que nenhum workspace está selecionado e exige escolha explícita do usuário. My Workspace foi listado; não foi selecionado nem usado para criar serviços. O responsável precisa confirmar o workspace antes de criar o receptor gratuito, validar HTTPS/OIDC/recibo e habilitar Testes/configuracao.json. Não declarar os relatórios da CI como se fossem envios dos PCs. O serviço planejado continua em docs/RECEPTOR.md. Depois desta entrega, parar para essa confirmação ou nova tarefa.

## Fix 0.4.4 — primeira parte do relatório

Base: main a5cfdd9f603674baf7ea67991a4ecb3edecb604f, após release 0.4.3. O PDF de bugs de seis páginas foi analisado; a divisão e pendências estão em BUGS.md. Nesta parte, somente o cadastro de espaços foi corrigido.

CrudService valida capacidade inteira positiva antes de persistir; form.html oferece min=1/step=1 e máximo compatível com Integer. CrudController captura RegraException somente para espaços, registra CADASTRO_INVALIDO/NEGADO e retorna o formulário com HTTP 400 e os valores enviados, incluindo checkboxes desligados. A transação de serviço já terminou e foi revertida antes dessa resposta. admin.js reabilita o envio desse formulário no evento pageshow para permitir a correção ao voltar pelo cache; a proteção contra cliques repetidos durante o envio é preservada.

EspacosCadastroIntegrationTest usa banco descartável próprio e requisições sem transação externa: verifica rejeições, rollback da edição, correção e persistência em outra requisição, limites na tela e log sem dados livres. test_admin_ui.cjs executa o JavaScript real e confere que o botão continua utilizável. Valores decimais financeiros são preservados.

Não há migration, limpeza, alteração de credenciais ou do diretório de dados. Render e envio automático do time continuam pendentes. A [0.4.4](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.4) foi publicada pelo [run 39025880429](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39025880429), com os 11 jobs aprovados e tag em `fbe03611991e1ce2678eab93615594e3e90223b1`. Foram conferidos os 12 arquivos e a preservação das oito releases anteriores. Passaram 91 testes Java (12 novos), 27 Go/go vet, cinco Node da agenda, dois da interface administrativa e sete do receptor, além do roteiro HTTP/backup e smoke nos seis ambientes. Detalhes e limites em VALIDACAO.md. Esta parte está concluída: parar e aguardar autorização para a próxima.

## Reorganização 0.4.3

A reorganização foi precedida por um ZIP de backup verificado com todas as versões, histórico, sete branches e sete tags. A árvore atual contém apenas os fontes ativos e seus testes/ferramentas de publicação. Java/POM estão em aplicativo/, o executável Go em iniciador/, o serviço Node em receptor/ e os guias em docs/. As pastas históricas de versões, configurações de VS Code, scripts de configurar/iniciar por terminal, exemplos de ambiente e SQL avulso saíram da árvore principal. Tags, releases, assets e histórico não foram apagados nem reescritos.

Scripts de teste/manutenção necessários ao roteiro continuam em aplicativo/scripts. O roteiro chama Maven pelo PATH, com Maven 3.9.9 preparado na CI por setup_maven.py e checksum SHA-512 oficial. Os workflows históricos foram retirados; o workflow ativo mantém os seis ambientes nativos e todos os gates. Dependabot acompanha somente o Maven ativo e GitHub Actions. Os nomes dos pacotes e o contrato bundle.json permanecem estáveis para os iniciadores já instalados.

Não houve alteração das regras de negócio, migrations, credenciais, permissões ou diretório de dados. O receptor continua desativado no arquivo público; ativá-lo é outra etapa e requer a confirmação pendente. Ao terminar esta reorganização com sua release, parar e aguardar a autorização do usuário antes de outra tarefa.

## Correção 0.4.1

A ausência de espaços ativos/disponíveis deixava `temLivre=false` na consulta pública, classificando todos os dias como indisponíveis. O endpoint `/disponibilidade` agora retorna `SEM_ESPACOS` nessa situação, inclusive para filtros sem resultado. O JavaScript mostra um estado neutro e orienta o cadastro/habilitação de espaços. Dias livres, parciais e efetivamente ocupados mantêm as regras anteriores. Não há migration, limpeza do banco, ativação forçada de espaços nem alteração das cinco contas.

A regressão é coberta por testes Java para banco novo, espaços inativos/desabilitados, filtro vazio, reservas parciais/integrais, cancelamento e mais de uma sala. Os testes da interface executam o JavaScript real em Node; o smoke do pacote verifica a transição de banco sem espaços para agenda livre após o cadastro.

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

# Handover técnico — InPETU Hub 0.4.2

Base desta entrega: `0d3f2a22c920b69675401ca401da8770b22eb0a9` (0.4.1). Base da linha 0.4: `12abbbcf9956ea68804229ac37ea527fb181f9df` (0.3.3). Entrega acadêmica de Projeto Aplicado do UniSENAI com o InPETU Hub. Equipe: Bernardo Rocha, João Mussiol, João Feitosa e Guilherme Batista. O responsável operacional e o aceite do parceiro ainda precisam ser definidos.

## Correção 0.4.1

A ausência de espaços ativos/disponíveis deixava `temLivre=false` na consulta pública, classificando todos os dias como indisponíveis. O endpoint `/disponibilidade` agora retorna `SEM_ESPACOS` nessa situação, inclusive para filtros sem resultado. O JavaScript mostra um estado neutro e orienta o cadastro/habilitação de espaços. Dias livres, parciais e efetivamente ocupados mantêm as regras anteriores. Não há migration, limpeza do banco, ativação forçada de espaços nem alteração das cinco contas.

A regressão é coberta por testes Java para banco novo, espaços inativos/desabilitados, filtro vazio, reservas parciais/integrais, cancelamento e mais de uma sala. Os testes da interface executam o JavaScript real em Node; o smoke do pacote verifica a transição de banco sem espaços para agenda livre após o cadastro.

## Sessões e relatórios 0.4.2

launcher/session.go cria a sessão após adquirir o lock, grava JSONL incremental e metadados de versão em cada evento, mantém o ID durante handoff e recupera interrupções. launcher/reporting.go consulta o destino público e retém os arquivos até confirmação final. SafeLogEncoder adiciona appVersion/launcherVersion/sessionId; SafeRoutes registra tipos de tela/ação sem parâmetros nem IDs na URI. Auditoria e autenticação mantêm códigos e IDs locais. O leitor do iniciador aceita somente campos permitidos da saída JSON do Java. PrivateLogDirectory aceita e protege as subpastas de sessões sem seguir links simbólicos; a regressão cobre as permissões e a preservação dos arquivos.

O receptor Node guarda fila temporária e não tem token de escrita do GitHub. O coletor autentica por OIDC com repositório, ID, main, workflow e audiência fixos. O GITHUB_TOKEN existe só no job e grava Testes/testeNN-data-id/log.txt e resumo.json. O índice recebidos.json mantém numeração global e deduplicação. Nunca executar conteúdo recebido como comando. A CI também publica sua versão e conclusão dos nove ambientes/gates antes da release.

A hospedagem requer confirmar o workspace no Render, publicar o receptor e ativar a URL real em Testes/configuracao.json. O arquivo público contém apenas enabled/endpoint, sem segredo. A ativação não exige reinstalar o executável. Sem receptor ativo, funcionam registros locais e relatórios de CI. Reinícios do servidor podem perder sua fila; a cópia local pendente permite reenvio na próxima abertura. Relatórios não atestam identidade do dispositivo.

Não houve migration, reset do banco nem troca de contas. Detalhes e limites: RELATORIOS.md, receiver/README.md e Testes/README.md. Validar sessões, versões, ausência de senhas, interrupção, fila offline, confirmação e handoff com o smoke do pacote. Testes Node cobrem receptor/coletor; o workflow confirma um token OIDC real. Não publicar logs operacionais brutos ou dados do perfil do usuário.

## Entrega

A 0.4 substitui a instalação por terminal/VS Code por pacotes nativos para Windows x64/ARM64, macOS Intel/Apple Silicon e Linux x64/ARM64. O iniciador Go abre uma interface local no navegador, instala o pacote completo no perfil do usuário, prepara chave/configuração, inicia/encerra o Java privado e consulta correções da linha 0.4.x. Não exige Java, Maven nem Go na máquina do usuário.

O aplicativo continua um monólito Spring Boot/Thymeleaf/JPA/H2, com Java 17/21, Flyway e os recursos da 0.3: agenda, reservas, CRM, permissões, logs, CSRF, sessões e backups. Migrations V1–V4 permanecem iguais. Apenas reservas aprovadas ocupam horários/consomem cota; conferência da identidade institucional e pagamentos continuam manuais. A 0.4 não declara atendimento integral ao PDF de melhorias funcionais.

A 0.4 usa banco novo no perfil do usuário, separado da pasta da 0.3. Não apaga nem importa automaticamente dados anteriores. Preserva estrutura/referências e começa sem cadastros ou reservas de exemplo. Cinco contas públicas são criadas no banco, com BCrypt, e restauradas ao iniciar; consulte ACESSOS.md. Não há arquivo de senhas obrigatório. Esse modo serve à demonstração local, não a dados confidenciais. Produção permanece um procedimento separado e não é implantada por esse iniciador.

## Código e operação

- `launcher/`: iniciador Go sem dependências externas; interface embutida, atualização, controle do processo, lock por instalação e teste de pacotes.
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

## Validação e limitações

A publicação exige testes Go, testes Java e o roteiro completo de regras/HTTP/backup, empacotamento e smoke do executável em seis runners nativos. Fonte, segredos, dependências e CodeQL são gates separados. O resultado de cada entrega fica em Actions; um teste local isolado não substitui a matriz.

Os testes verificam os binários e a interface HTTP; não substituem teste visual com pessoas nem comprovam todas as versões de Windows, macOS e distribuições Linux. Os executáveis não têm assinatura comercial/notarização; o sistema pode pedir confirmação, e políticas corporativas podem bloquear. Um navegador padrão e ambiente desktop são necessários à interface. Não foi realizada implantação MySQL/SMTP/TLS em produção. Mantém-se a limitação conhecida de resultados CodeQL: ausência de alertas não significa ausência de falhas nem garante execução completa de todas as consultas.

## Próximas entregas

Atualizar VERSION, POM, caminhos de JAR dos scripts de desenvolvimento e RELEASE-NOTES. O build passa a versão ao Go. Preservar o protocolo de `bundle.json`, nomes dos assets, armazenamento da linha 0.4 e compatibilidade de migrations. Corrigir vulnerabilidades de runtime também exige gerar e publicar novo pacote; Java privado não é atualizado pelo gerenciador do sistema. A 0.3 permanece histórica e seu workflow não publica a 0.4. Use a release/commit recebido como referência para manutenção.

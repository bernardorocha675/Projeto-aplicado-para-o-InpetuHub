# Handover técnico — InPETU Hub

Documento de referência para a equipe que receberá, executará e manterá o sistema. Reúne o estado da entrega, as decisões que afetam a operação e os procedimentos necessários para dar continuidade ao projeto.

| Identificação | Referência |
|---|---|
| Entrega | **Versão 0.3** — pasta `versão 0.3/` |
| Versão do pacote Java | **0.3.0** — definida em [pom.xml](pom.xml) |
| Data da revisão | 15/10/2026 |
| Base desta revisão | `9a567868d44efb1c1b7cb6d5de36ed4084fad0ed`; identificação final pela tag `v0.3.0` |
| Contexto | Disciplina de Projeto Aplicado do UniSENAI, em parceria com o InPETU Hub |
| Estado da entrega | Projeto acadêmico com execução local e testes automatizados validados; sem implantação em produção realizada nesta entrega |

A numeração pública foi padronizada: as antigas pastas `versão 01`, `versão 02` e `versão 03` agora são `versão 0.1`, `versão 0.2` e `versão 0.3`. Os pacotes Java acompanham a numeração `0.1.0`, `0.2.0` e `0.3.0`. Os nomes do banco, as migrations e o formato de backup foram preservados. As novas entregas seguem o processo de [Releases](../RELEASES.md), com notas e pacote público após as verificações automáticas.

## 1. Responsáveis e continuidade

A equipe acadêmica é formada por **Bernardo Rocha, João Mussiol, João Feitosa e Guilherme Batista**. O InPETU Hub é a instituição parceira.

O responsável pela manutenção após a entrega, o administrador do ambiente e o canal de suporte precisam ser definidos no recebimento. Este documento não atribui essas funções a um aluno específico nem representa aceite formal do parceiro.

O repositório contém o código e a documentação. Contas, senhas, banco em uso, chave MFA e configurações reais são entregues separadamente ao responsável autorizado. A transferência deve incluir acesso individual ao repositório e aos serviços efetivamente utilizados, sem publicar segredos em documentos ou issues.

## 2. Produto e escopo entregue

O sistema oferece agenda pública de espaços e uma área interna para reservas e relacionamento com empreendedores. A versão 0.3 mantém as proteções e regras da versão 0.2 e acrescenta logs técnicos persistentes, diagnóstico/recuperação local de acessos e exibição opcional de senhas nos formulários.

| Área | Comportamento disponível |
|---|---|
| Agenda pública | Consulta de disponibilidade e envio de solicitações de reserva. |
| Conferência de solicitações | Fila interna para verificar identidade e vínculo institucional antes de encaminhar o pedido para reserva. |
| Gestão interna | Cadastros, reservas, histórico de relacionamento, cotas e registro manual de pagamentos. |
| Autenticação | Contas privadas, BCrypt, perfis ADMIN/COLABORADOR, sessões com expiração e MFA obrigatório para administradores. |
| Auditoria | Registro transacional de autenticação e operações de negócio no banco. |
| Diagnóstico | Logs em arquivo, correlação por requisição e relatórios de testes. |
| Recuperação local | Backup/restauração H2 cifrados e conferência/recuperação explícita das contas de um arquivo privado. |

As pastas versão 0.1 e versão 0.2 permanecem como entregas anteriores. Novas alterações devem partir da versão 0.3; este handover não comprova atendimento integral dos requisitos adicionais do PDF de melhorias.

### Regras de negócio que devem ser preservadas

1. A solicitação pública fica separada do CRM em `solicitacao_publica`; seu envio não bloqueia salas nem consome cotas.
2. Para CNPJ existente, a confirmação de email usa o contato institucional cadastrado. A confirmação de email, sozinha, não comprova representação da instituição.
3. A equipe precisa conferir o vínculo por contato confiável e registrar a justificativa. Em produção, a confirmação de email também é obrigatória.
4. Apenas a reserva **APROVADA** ocupa horário e consome cota. A aprovação recalcula disponibilidade, cotas e preços sob bloqueio transacional.
5. Solicitações recebidas e reservas pendentes expiram após 24 horas. A rotina verifica a expiração a cada minuto; a aprovação também verifica o prazo.
6. Recusar/cancelar pendente sem alocação não deve gerar crédito fictício de cota. Pagamentos são registrados manualmente, sem integração com um provedor de cobrança.

## 3. Arquitetura e pontos de manutenção

A aplicação é um monólito Java com páginas renderizadas pelo servidor. O navegador acessa controllers Spring MVC; os serviços aplicam as regras; repositories JPA persistem os dados. Spring Security protege as requisições, Flyway gerencia migrations e Thymeleaf renderiza as telas.

| Componente | Tecnologia / localização |
|---|---|
| Runtime e build | Java 17 ou 21; Maven 3.9.9 preparado pelos scripts. |
| Framework | Spring Boot 4.1.1, Spring Security, Spring MVC, JPA/Hibernate, Thymeleaf e Bean Validation. |
| Banco local | H2 em arquivo; dispensa instalar um servidor MySQL para apresentação local. |
| Perfil de produção | MySQL, HTTPS direto e SMTP; a infraestrutura real ainda precisa ser configurada e validada. |
| Interface | `src/main/resources/templates/` e `src/main/resources/static/`; scripts e estilos locais. |
| Estrutura do banco | `src/main/resources/db/migration/`; migrations V1, V2 e V3. Não editar migrations já aplicadas. |
| Entrada da aplicação | `src/main/java/br/com/inpetu/hub/InpetuApplication.java`; mantém o lock da instalação e separa os modos offline de acesso da inicialização do servidor. |

Os pacotes abaixo ficam em `src/main/java/br/com/inpetu/hub/`:

| Pacote / arquivo | Responsabilidade e pontos importantes |
|---|---|
| `controller/` | Rotas públicas/internas, formulários e encaminhamento para serviços. |
| `service/PublicRequestService.java` | Solicitações, confirmação e conferência antes da criação da reserva. |
| `service/ReservaService.java`, `DisponibilidadeService.java`, `CotaService.java`, `PrecoService.java` | Aprovação, concorrência, agenda, cotas e cálculo de valores. |
| `service/CrudService.java` | Cadastros, validação e revogação de sessões após mudanças sensíveis de conta. |
| `service/ExpirationService.java` | Expiração periódica de pendentes. |
| `security/` | Autenticação, perfis, sessão, MFA, criptografia, limites e auditoria. |
| `config/PrivateAccountInitializer.java` | Importação privada única, controlada pela tabela `bootstrap_seguro`. |
| `config/DeploymentGuard.java` | Validação de perfis e requisitos de produção. |
| `maintenance/LocalAccessTool.java` | Conferência/recuperação offline do H2, com confirmação, transação e auditoria. |
| `observability/` | Eventos controlados, encoder seguro, requestId, permissões e retenção dos logs. |

O botão Mostrar/Ocultar senha está em `static/js/password-toggle.js`, carregado pelo fragmento comum das páginas. Ele altera apenas a visibilidade, preserva o valor e volta a ocultar ao enviar o formulário, trocar de aba ou sair da página.

## 4. Preparar e executar localmente

O procedimento completo, inclusive ferramentas básicas por sistema e problemas comuns, está em [COMO-INICIAR.md](COMO-INICIAR.md).

1. Clone/extraia o repositório inteiro e abra **versão 0.3** como pasta no VS Code. Execute os comandos no terminal dessa pasta.
2. Para usar as contas da equipe em um banco novo, coloque o pacote privado em `.secrets/accounts.properties` **antes do primeiro início**. Clonar o código não copia contas nem registros.
3. Execute o configurador e aguarde sua conclusão. Ele reutiliza um JDK 17/21 compatível ou baixa ferramentas locais com verificação de checksum; não exige instalação global do Maven. Requer internet para downloads e dependências.
4. Execute o comando de iniciar, aguarde `event=SERVER_READY` e abra http://localhost:8080/. Se mudar `PORT`, use a porta correspondente.

| Ação | Linux / macOS | Windows PowerShell / CMD |
|---|---|---|
| Configurar | `bash configurar.sh` | `.\configurar.bat` |
| Iniciar | `bash iniciar.sh` | `.\iniciar.bat` |
| Testar, com servidor parado | `bash testar.sh` | `.\testar.bat` |
| Conferir acessos, com servidor parado | `bash acessos.sh verificar` | `.\acessos.bat verificar` |

Para parar, pressione Ctrl+C no terminal do servidor. As tarefas do VS Code estão em `.vscode/tasks.json`. Os scripts de iniciar recompilam o JAR sem repetir os testes e selecionam o perfil **local**, limitado ao próprio computador. Não usá-los como procedimento de produção.

O configurador não imprime credenciais. Se não houver pacote privado, cria um administrador aleatório para aquela instalação. O exemplo local habilita dados fictícios por `SEED_DEMO=true`; confirme parâmetros e remova a dependência de dados de demonstração antes de uso real.

## 5. Configuração e arquivos que precisam ser preservados

| Arquivo / variável | Finalidade |
|---|---|
| `.env.local` | Configuração privada local; inclui `PORT`, `LOCAL_DB_PASSWORD` e opções de demonstração/email. |
| `.env` | Configuração privada usada pelo perfil de produção. |
| `.secrets/accounts.properties` / `ACCOUNTS_FILE` | Arquivo de contas iniciais ou de recuperação. Não é uma sincronização permanente de usuários. |
| `.secrets/mfa.key` / `MFA_KEY_FILE` | Chave de 32 bytes em Base64 URL para proteger segredos MFA. Não regenerar em uma instalação com MFA já configurado. |
| `data/inpetu-v02.mv.db` | Banco local com usuários, registros e auditoria. O nome técnico foi mantido para compatibilidade com a versão 0.2. |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexão MySQL de produção. |
| `MAIL_USERNAME`, `MAIL_PASSWORD` e `spring.mail.*` | SMTP. Ajustar as propriedades do servidor se usar outro provedor. |
| `TLS_KEY_STORE`, `TLS_KEY_STORE_PASSWORD` | Certificado PKCS12 e senha para HTTPS de produção. |

Os arquivos `.env.example` e `.env.local.example` documentam os formatos, sem valores secretos reais. Nunca preencher os exemplos versionados com credenciais de uso.

Preserve juntos o banco e sua configuração, além da chave MFA. Os arquivos de contas contêm senhas iniciais em texto e ficam sob custódia privada do responsável; depois do provisionamento, podem ser retirados da instalação e guardados em local seguro, desde que a chave MFA permaneça. Diagnóstico/recuperação exigem disponibilizar novamente o arquivo correto. A senha atual pode ser diferente da inicial se o usuário já a alterou.

## 6. Acessos e segurança

| Perfil | Acesso esperado |
|---|---|
| Visitante | Agenda e fluxo público de solicitação. |
| COLABORADOR | Operação interna; não acessa as funções administrativas protegidas por ADMIN. MFA é opcional até ser ativado. |
| ADMIN | Administração de contas/configuração, auditoria e recuperação administrativa de MFA; segundo fator obrigatório. |

Não existe login público padrão. As senhas são armazenadas com BCrypt, custo 12. As sessões têm 20 minutos de inatividade e limite absoluto de duas horas; o segundo fator deve ser concluído em até cinco minutos após o login. Mudança de senha, login, perfil, situação ou versão de segurança revoga sessões anteriores.

Administradores devem cadastrar TOTP e guardar os oito códigos de recuperação de uso único. Os segredos MFA são cifrados e os códigos de recuperação não são armazenados em texto. A recuperação local de senhas não remove MFA nem muda o perfil de uma conta existente. Perda de todos os fatores administrativos exige procedimento operacional com o responsável pelo banco ou restauração adequada; não há rota pública de desbloqueio.

CSRF, escape HTML, consultas parametrizadas, CSP e cabeçalhos de segurança permanecem ativos. O login admite até dez tentativas por conta e sessenta por origem em janela de quinze minutos; há limites adicionais por fluxo. A limitação fica na memória de uma instância. Múltiplas instâncias e uso de proxy reverso exigem revisão de infraestrutura; cabeçalhos X-Forwarded-* não são confiados pela configuração entregue.

### Login recusado ou pacote importado depois do primeiro início

Siga [ACESSOS.md](ACESSOS.md). A importação inicial é feita uma vez: substituir o arquivo de contas depois disso não redefine automaticamente as senhas.

Com o servidor parado, o comando `acessos.sh verificar` / `acessos.bat verificar` compara o arquivo privado com o banco e informa apenas quantidades, sem credenciais. Se for necessário recuperar, faça backup primeiro e execute `acessos.sh recuperar` / `acessos.bat recuperar`. A confirmação exata é `RECUPERAR COM BACKUP`.

A recuperação importa contas ausentes e redefine/reativa as listadas; mantém contas fora do arquivo, perfis existentes, MFA e registros de negócio. É transacional e auditada como `ACESSO_RECUPERADO_LOCAL`. A confirmação declara que o operador fez o backup; não gera um backup automaticamente. Esses comandos atendem somente o H2 local, sem iniciar HTTP.

## 7. Rotina de operação e diagnóstico

| Necessidade | Onde consultar |
|---|---|
| Agenda / login | `/` e `/login`. |
| Solicitações recebidas | `/admin/solicitacoes`. |
| Operação de reservas | `/admin/reservas`. |
| Auditoria administrativa | `/admin/config/auditoria`, com ADMIN e MFA; a tela mostra os últimos 200 eventos, sem apagar os anteriores do banco. |
| Logs técnicos | `logs/inpetu.log`; guia de leitura em [LOGS.md](LOGS.md). |
| Resultado do executor | `relatorios-testes/ultimo.html` e `ultimo.txt`. |

A expiração de pendentes e a limpeza de logs rodam dentro do processo. Se o servidor estiver parado, as rotinas não executam; a aprovação também verifica se o prazo expirou.

Os logs usam JSON por linha, horário UTC, método/status/duração e UUID gerado pelo servidor em `X-Request-ID`. Não registram URI, parâmetros, corpos, IP, cookies ou cabeçalhos recebidos. Eventos de auditoria espelhados no arquivo são emitidos após commit e omitem detalhes livres. Mensagens livres de frameworks aparecem como `FRAMEWORK_EVENT`; exceções mantêm tipo e até vinte pontos da pilha, sem mensagens textuais. Saídas diretas da JVM não passam pelo encoder.

Há rotação diária UTC ou por aproximadamente 10 MB, com compactação gzip. A retenção própria mantém as quatorze datas UTC mais recentes e aplica um limite de 200 MB aos arquivos antigos elegíveis. A limpeza ocorre na prontidão e a cada minuto, preservando arquivos modificados há menos de um minuto; ativo e compactação recente ficam fora do limite temporariamente. Não é quota rígida de disco.

Para investigar um erro, anote horário, passos, resultado esperado e requestId; procure as linhas correspondentes e compartilhe apenas o trecho revisado. Não envie arquivos privados, corpo completo de requisição ou o banco. Não habilite logs de parâmetros SQL/HTTP/SMTP para tentar diagnosticar sem revisar a confidencialidade. Monitore espaço e falhas de gravação; não há coleta centralizada nem alertas automáticos nesta entrega.

## 8. Backup e restauração H2

O programa exige servidor parado e usa o lock da instalação. O backup é cifrado com AES-GCM; a chave é derivada da senha por PBKDF2-HMAC-SHA256, com 310 mil iterações. Inclui banco, `.env.local`, chave MFA e arquivo inicial de contas quando presente. Logs técnicos e código-fonte não fazem parte do arquivo; a auditoria do banco é incluída com o banco.

Na pasta versão 0.3, com uma instalação já configurada:

```sh
# Linux/macOS. Escolha outro nome se o arquivo já existir.
bash backup.sh backup .secrets/antes-atualizacao.inpetu
```

No Windows: `.\backup.bat backup .secrets\antes-atualizacao.inpetu`. Informe uma senha de pelo menos dezesseis caracteres quando solicitada, nunca como argumento. Guarde uma cópia do backup fora da instalação, em local privado, e a senha separadamente. Sem a senha não há recuperação.

Para restaurar:

1. Prepare uma pasta nova com o código da versão 0.3, sem banco, `.env.local` ou chave MFA existentes.
2. Disponibilize Java 17/21 pelo sistema ou por `JAVA_HOME`, antes de executar o configurador. Pode reutilizar o JDK da instalação anterior; no macOS, use a pasta que contém `bin/java`, normalmente `Contents/Home`.
3. A partir da pasta nova, execute `bash backup.sh restore /caminho/privado/backup.inpetu` ou `.\backup.bat restore C:\caminho\backup.inpetu` e informe a senha no terminal.
4. Após restaurar, execute o configurador, inicie o sistema e confira login/MFA, registros e auditoria. O configurador preserva os arquivos restaurados.

A ferramenta verifica a autenticidade do arquivo e as entradas antes da cópia, recusa sobrescrita e limita o conteúdo restaurado a 1 GiB. Faça a restauração de ensaio antes de depender do backup. MySQL exige dump/restore e procedimento próprio do servidor; esta ferramenta não cobre esse banco.

## 9. Atualização e retorno à versão anterior

### Da versão 0.2 para a versão 0.3

Não há nova migration de banco na versão 0.3: ela mantém V1–V3, o nome H2 e o formato de backup da versão 0.2.

1. Registre o commit em uso e pare a versão 0.2. Faça e confira o backup cifrado.
2. Restaure primeiro em uma instalação separada da versão 0.3 conforme a seção anterior. Não renomeie `inpetu-v02.mv.db`.
3. Configure e inicie essa cópia. O banco restaurado conserva usuários, senhas atuais, MFA e marcador de importação; não reimporte contas nem regenere a chave.
4. Confira agenda, reservas, cotas, permissões e logs. Defina a janela de troca com o responsável pelos dados e evite novas gravações durante a migração final.
5. Mantenha a instalação/backup anteriores para retorno. Não execute as duas versões simultaneamente sobre o mesmo banco.

Para voltar, pare a versão 0.3 e restaure o backup validado em uma instalação separada da versão 0.2 com a configuração correspondente. Alterações posteriores ao backup precisam de tratamento antes do retorno para evitar perda de registros. Migração a partir da versão 0.1 exige o procedimento específico do [handover da versão 0.2](../vers%C3%A3o%200.2/HANDOVER.md), que altera regras de contas e cotas.

### Atualizações dentro da versão 0.3

**Troca do nome da pasta `versão 03` para `versão 0.3`:** pare o servidor e faça o backup antes de atualizar. O Git renomeia o código rastreado, mas arquivos privados ignorados podem permanecer na pasta antiga. Transfira de forma privada `data/`, `.env.local` e a pasta `.secrets/` completa para a nova pasta, mantendo a chave MFA original. Preserve também os backups e logs anteriores em local privado. Não execute as duas pastas ao mesmo tempo nem inicie a nova com banco vazio se pretende continuar a instalação existente. Após configurar as ferramentas na pasta nova, confira os acessos e os registros antes de descartar qualquer cópia antiga. Não há alteração de estrutura de banco nesta renomeação.

Preserve `data/`, `.env.local` e `.secrets/`; atualize o código com o servidor parado, execute o configurador/testes e inicie novamente. Não substitua toda a instalação por uma pasta vazia nem copie ferramentas/relatórios temporários como se fossem dados do sistema. O JAR continua identificado como `0.3.0`; use também o SHA do commit para distinguir as correções desta entrega.

## 10. Testes e evidências de validação

Depois de configurar e com o servidor parado, use `bash testar.sh` ou `.\testar.bat`. O executor usa Java, contas sintéticas e um banco H2 isolado. Os relatórios HTML/TXT ficam fora do Git; não dependem das contas privadas da instalação normal. Python é necessário somente para verificações auxiliares específicas, não para esse comando.

| Evidência | Resultado e alcance |
|---|---|
| Rodada local mais recente | **76 verificações aprovadas**, incluindo **58 casos JUnit**, HTTP real, login/MFA, concorrência, persistência, logs, backup e recuperação local de contas. |
| Compatibilidade nativa | [Execução 38792529766](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/38792529766), no código `a73bcc421699db4f99a7971bd29ea76a0de500cc`: nove jobs aprovados. Ubuntu x64/ARM64, macOS Apple Silicon/Intel e Windows x64/ARM64 executaram o configurador e o roteiro completo, incluindo os modos de acesso via JAR. |
| Acessos privados | As cinco credenciais entregues foram aceitas por HTTP em banco novo e isolado da versão 0.3, sem expor valores ou alterar o banco de trabalho. Isso não diagnostica automaticamente a instalação de outro computador. |
| Interface de senha | Sintaxe e comportamento JavaScript conferidos com DOM simulado; não há comprovação de teste gráfico completo no navegador. |
| Segredos e dependências | Verificação de arquivos privados e Gitleaks aprovados; consulta de cem dependências de execução sem alertas naquela rodada. Novos avisos podem surgir depois. |
| CodeQL | Zero resultados no SARIF, com erro interno ao renderizar um caminho da consulta `java/csrf-unprotected-request-type`. Essa consulta não é evidência completa; há testes de integração de CSRF. |

A automação está no [workflow da versão 0.3](../.github/workflows/security-v0.3.yml); Dependabot acompanha dependências e ações fixadas por SHA. Os relatórios de teste são enviados à CI sem bancos, contas, chaves ou logs privados. O job de Gitleaks também pode gerar seu próprio SARIF.

Consulte [TESTAR.md](TESTAR.md) para a cobertura manual e [docs/VALIDACAO.md](docs/VALIDACAO.md) para o histórico das rodadas. As referências a README vazio nesse histórico descrevem o estado daquela rodada; o README principal foi atualizado posteriormente. Esta revisão do handover é documental e não representa nova rodada de testes do aplicativo.

## 11. Produção e integrações

O perfil `prod` existe, mas a implantação real não foi realizada nesta entrega. Antes de operar, o responsável precisa fornecer MySQL, configuração SMTP, certificado PKCS12, contas autorizadas e chave MFA, além de definir rotina de backup e acompanhamento do serviço.

Compile o projeto e execute o JAR com a configuração privada de produção disponível:

```sh
java -jar target/inpetu-hub-0.3.0.jar --spring.profiles.active=prod
```

O perfil exige HTTPS direto, cookie Secure, confirmação de email e SMTP; desabilita demonstração. Não combinar `local` e `prod`. O exemplo MySQL usa verificação de identidade do certificado e precisa corresponder ao servidor real. Valide certificado, entrega de email, permissões do banco, firewall e recuperação no ambiente de destino.

Não há configuração entregue de serviço do sistema, hospedagem, domínio ou balanceador. Esses componentes e seus responsáveis devem ser documentados quando forem escolhidos. O uso de proxy/múltiplas instâncias exige decisões adicionais sobre confiança de cabeçalhos, TLS e limites compartilhados. As chaves e senhas permanecem fora do repositório.

## 12. Limitações e pendências conhecidas

| Item | Situação / próximo passo |
|---|---|
| Requisitos adicionais do PDF | Melhorias funcionais/visuais, como fotos das salas e adequações de rodapé, continuam fora desta entrega. Rastrear os itens e validar com o parceiro antes de considerar o escopo completo. |
| Regras e valores de operação | Parâmetros financeiros, preços, expediente, feriados e dados de demonstração precisam de confirmação do InPETU Hub. |
| SMTP | Há envio de email, sem fila persistente/reenvio automático. Falhas de notificação não desfazem reservas confirmadas. Entrega real ainda precisa de validação. |
| Infraestrutura | TLS/MySQL reais, implantação e restauração de produção não foram validados. |
| Navegador | Testes HTTP e DOM simulado não substituem conferência visual, navegação por teclado e uso do autenticador no celular. |
| Observabilidade | Logs locais não são cifrados nem invioláveis; administradores do sistema podem acessá-los. Não há métricas/painel ou alertas externos. |
| Retenção de dados | A rotação técnica não elimina auditoria do banco nem registros do CRM. O responsável deve definir retenção e acesso aos dados e backups. |
| Continuidade | Definir mantenedor, operador, canal de suporte e aceite do parceiro. |

## 13. Conferência de recebimento

A equipe recebedora deve executar e registrar esta conferência em ambiente de teste. Os itens abaixo não estão marcados como aceitos por terceiros apenas porque os testes automatizados passaram.

- [ ] Acessar o repositório e identificar a pasta versão 0.3, o pacote 0.3.0 e o commit recebido.
- [ ] Preparar uma instalação limpa pelo guia, sem depender de arquivos não documentados do computador de quem desenvolveu.
- [ ] Receber os arquivos privados por canal autorizado e conferir login, MFA e separação entre ADMIN/COLABORADOR.
- [ ] Percorrer uma solicitação até conferência/aprovação e validar agenda, cota, histórico e valores com o parceiro.
- [ ] Executar o roteiro automático e revisar o relatório e as verificações manuais pendentes.
- [ ] Criar um backup e restaurá-lo em outra pasta, comprovando recuperação dos dados e acesso.
- [ ] Localizar um evento de auditoria e um requestId no log e preparar um relato de erro sem credenciais.
- [ ] Conhecer o procedimento de recuperação de contas e confirmar que ele preserva MFA/perfis existentes.
- [ ] Definir responsáveis, pendências aceitas e os requisitos para eventual produção.

Registrar no recebimento: **data, commit, ambiente, responsável pela entrega, responsável pelo recebimento, resultados e pendências**. Não há aceite formal ou prazo de suporte estabelecido por este documento.

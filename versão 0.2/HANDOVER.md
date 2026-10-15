# Handover — InPETU Hub versão 0.2

Data: 15/10/2026. Base: versão 0.1 do commit `08f2de3385c13adc1f5313afaa56e73021a4cf0f`. A versão 0.1 e o README vazio da raiz foram preservados. O pacote Java passa a `0.2.0`. A finalidade desta entrega é a segurança; não representa conclusão dos novos requisitos visuais do PDF.

## Instalação e contas

Leia [COMO-INICIAR.md](COMO-INICIAR.md). Foram mantidos dois comandos: configurar e iniciar, em Linux/macOS/Windows. Scripts verificam checksums, usam ferramentas locais e preservam arquivos existentes. O banco local da versão 0.2 tem nome próprio: `data/inpetu-v02.mv.db`.

As contas particulares, nomes e senhas iniciais estão em um pacote entregue em privado, não neste repositório. O papel administrativo e os papéis de colaboradores também são definidos nesse arquivo. Uma instalação sem arquivo de contas recebe um administrador aleatório, cujo login e senha ficam em `.secrets/accounts.properties` com permissões restritas. Esse administrador pertence apenas ao banco criado nessa instalação.

O provisionamento valida todas as entradas antes de escrever, exige logins únicos e pelo menos um administrador, armazena senhas BCrypt e é marcado como concluído na tabela `bootstrap_seguro`. Reiniciar não redefine senhas nem reativa usuários. Depois da primeira importação, o arquivo de senhas pode ser retirado da máquina, mantendo uma cópia privada se necessário; `.secrets/mfa.key` deve permanecer.

Nunca versionar `.secrets/`, `.env`, `.env.local`, `data/`, certificados, backups ou arquivos com acessos. Não preencher os exemplos versionados com dados reais. Os testes usam contas sintéticas que não acessam bancos reais.

## Mudanças e decisões

| Área | Implementação |
|---|---|
| Autenticação | BCrypt; sem conta padrão; tamanho de senha controlado; limites temporários por conta e origem. |
| Sessões | Principal com ID/versão de segurança; conferência com banco a cada requisição; alterações de conta revogam acesso; 20 minutos por inatividade e duas horas absolutas. |
| Segundo fator | TOTP obrigatório para administradores, opcional para colaboradores; chave cifrada AES-GCM; proteção contra repetição; oito códigos de recuperação de uso único. |
| Permissões | ADMIN gerencia contas, parâmetros, auditoria e recuperação MFA; COLABORADOR mantém a operação do CRM. CSRF continua ativo. |
| Solicitação pública | Snapshot em `solicitacao_publica`, sem criar/vincular cadastro de empreendedor antes da conferência da equipe. Idempotência por chave de submissão. |
| Identidade | Confirmação por código de email quando configurado; CNPJ existente usa contato já registrado. Equipe precisa atestar o vínculo por contato confiável. Email confirmado não comprova representação de empresa. |
| Cotas e agenda | Somente aprovadas bloqueiam horário e consomem horas. Aprovação mantém lock transacional e recalcula preços/cotas. Recusa de pendente não cria estorno fictício. |
| Expiração | Solicitações e reservas pendentes têm prazo de 24 horas; rotina a cada minuto; aprovação também rejeita pendente expirada. |
| Auditoria | Eventos estruturados de login, revogação, MFA, CRUD e disponibilidade. Autor/ID/ação/resultado, sem senhas, hashes, tokens, login particular ou corpos de formulário. |
| Navegador | CSP restritiva, Referrer-Policy, Permissions-Policy, escape HTML e recursos locais. |
| Produção | Perfil explícito prod, TLS direto, cookie Secure, SMTP obrigatório e confirmação de email. Encaminhamentos de proxy não são confiados. |
| Backup | Arquivo cifrado AES-GCM, chave derivada de senha com PBKDF2-HMAC-SHA256 e 310 mil iterações; restauração autentica o conteúdo antes de copiar arquivos. |
| Verificações contínuas | CI nativa em seis plataformas, Gitleaks, CodeQL e consulta OSV do conjunto de dependências resolvidas; ações fixadas por SHA. |

Mudar o status PENDENTE foi uma decisão de segurança deliberada. A ordem de envio não garante mais um bloqueio de sala ou uma alocação de cota. A aprovação define a alocação após a conferência. Os testes de concorrência foram transferidos para essa fronteira.

## Arquivos e manutenção

- `security/`: configuração HTTP, principal, guarda de sessão, limites, TOTP, criptografia e auditoria.
- `config/PrivateAccountInitializer.java`: importação privada única e revogação de contas anteriores durante migração.
- `config/DeploymentGuard.java`: recusa configuração de teste fora dos testes e produção sem os requisitos obrigatórios.
- `controller/AccountController.java`: segundo fator, alteração de senha e recuperação administrativa.
- `service/PublicRequestService.java` e `controller/RequestReviewController.java`: intake público, confirmação e conferência da equipe.
- `service/ReservaService.java`, `DisponibilidadeService.java`, `CotaService.java`: integridade de aprovação, conflitos e cotas.
- `service/CrudService.java`: validação, auditoria de alterações e incremento da versão de segurança.
- `service/ExpirationService.java`: expiração de solicitações/pendentes.
- `db/migration/V3__seguranca.sql`: novas tabelas e campos e compensação das alocações provisórias antigas. V1/V2 foram preservadas.
- `scripts/PrepareLocal.java`: geração local de segredos e permissões POSIX/ACL, sem expor valores.
- `scripts/Backup.java` e wrappers: backup/restauração H2 cifrados.
- `scripts/check_secrets.py`, `check_dependencies.py`, `smoke.py`, `test_backup.py`: verificações auxiliares; requerem Python 3.
- `.github/workflows/security-v0.2.yml`: verificações contínuas. Não habilita deploy.

## Migração de um banco existente

1. Pare a versão 0.1 e confirme que não existem outras execuções ou cópias em uso. Faça uma cópia privada completa do banco e configuração anteriores. Não execute as duas versões no mesmo banco.
2. Teste a migração primeiro em uma cópia. A versão 0.2 usa outro nome de arquivo H2 por padrão e não importa registros automaticamente. Para migrar H2, copie o banco parado para `data/inpetu-v02.mv.db` na instalação nova; configure a senha original desse banco em `.env.local`. Não use a senha aleatória criada para um banco diferente.
3. Copie o arquivo privado com as novas contas antes de iniciar. Migrations e Hibernate validam a estrutura. V3 compensa alocações provisórias de cotas da versão 0.1; solicitações pendentes deixam de bloquear a agenda.
4. No primeiro provisionamento da versão 0.2, todas as contas anteriores são desativadas, os novos acessos são importados e suas versões de segurança mudam. Históricos e relacionamentos dos usuários anteriores são preservados. Um login coincidente recebe a nova credencial explicitamente provisionada.
5. Confira reservas, cotas, pagamentos e contas na cópia. Configure o autenticador administrativo e guarde os códigos de recuperação. Teste também backup e restauração antes de migrar o banco de trabalho.
6. Para voltar à versão 0.1, restaure o backup anterior e a configuração correspondente. Não execute a versão 0.1 sobre o banco já migrado. Migrations aplicadas não devem ser editadas.

MySQL requer o mesmo procedimento com dump/restore e as mesmas migrations Flyway, usando credenciais externas. O procedimento de backup Java atende apenas ao H2 local; use a ferramenta apropriada do servidor MySQL para produção.

## Backup e restauração H2

Pare o servidor. O lock da instalação impede backup/restauração simultâneos à execução pelo launcher.

```sh
# Linux/macOS: informe a senha quando o terminal pedir.
bash backup.sh backup /caminho/privado/arquivo.inpetu
# Restauração: executar a partir de uma cópia nova do código, sem banco/configuração existentes.
bash backup.sh restore /caminho/privado/arquivo.inpetu
```

No Windows use `.\backup.bat backup C:\caminho\arquivo.inpetu` ou `.\backup.bat restore C:\caminho\arquivo.inpetu`. A senha não vai na linha de comando; use pelo menos 16 caracteres e guarde-a separadamente. Uma senha incorreta ou arquivo adulterado é recusado antes de escrever os arquivos de destino. A ferramenta não substitui arquivos existentes e limita o tamanho restaurado a 1 GiB.

Na pasta nova de restauração, disponibilize primeiro Java 17/21 pelo sistema ou por `JAVA_HOME`; não execute ainda o configurador, pois ele criaria arquivos privados que a restauração se recusaria a sobrescrever. Você pode reutilizar o JDK baixado na instalação anterior: `export JAVA_HOME="/caminho/anterior/.tools/jdk"` no Linux; no macOS use a pasta que contém `bin/java` (normalmente `.tools/jdk/Contents/Home`). No PowerShell, use `$env:JAVA_HOME='C:\caminho\anterior\.tools\jdk'`. Copie apenas o código para o destino novo e execute o comando de restauração a partir dele. Após restaurar, execute o configurador para baixar Maven, compilar e testar; ele preserva a configuração e a chave restauradas. Em seguida, execute o comando de iniciar.

O backup contém banco, configuração e chave MFA; pode incluir o arquivo inicial de contas se ele ainda estiver na instalação. O arquivo completo é cifrado. Sem a senha do backup não há recuperação. Não publique o arquivo nem sua senha.

## Produção

Não exponha o perfil local à rede. Para produção, forneça `.env` privado, uma chave MFA de 32 bytes codificada em Base64 URL no arquivo privado, as contas iniciais, MySQL, SMTP e certificado PKCS12. Rode o JAR com `--spring.profiles.active=prod`. O código não contém certificado, domínio, senha SMTP ou senha de banco.

`TLS_KEY_STORE`, `TLS_KEY_STORE_PASSWORD`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MAIL_USERNAME` e `MAIL_PASSWORD` ficam fora do Git. O exemplo MySQL solicita verificação de certificado/identidade do servidor; o servidor MySQL precisa estar configurado para isso. Planeje conta de migração separada da conta de execução e permissões mínimas com o responsável pelo banco.

A configuração entregue usa TLS direto. Um proxy reverso exige uma revisão explícita de confiança de cabeçalhos, certificado, IP de origem e limitação de tráfego; não habilite confiança geral em X-Forwarded-* só para fazer o login funcionar. Os limites em memória atendem uma instância; múltiplas instâncias exigem estado compartilhado ou limitação na infraestrutura.

## Recuperar acesso

O usuário com MFA ativo pode usar um dos códigos de recuperação. Um administrador autenticado pode redefinir o segundo fator de outro colaborador no painel após conferir a identidade e informar a própria senha; a ação revoga as sessões do alvo e é auditada. Perder todos os fatores de todos os administradores exige recuperação operacional pelo responsável pelo banco ou restauração de backup validado — não existe senha mestre ou rota pública de desbloqueio.

Para trocar o conjunto de contas de um banco em uso, não edite o marcador de bootstrap sem um procedimento controlado e backup. Faça as alterações pelo painel administrativo, que revoga sessões e registra o evento.

## Validação e limites

Consulte [docs/VALIDACAO.md](docs/VALIDACAO.md) e [docs/SEGURANCA.md](docs/SEGURANCA.md). Verificação de dependências deve ser repetida: uma versão sem alertas hoje pode receber um aviso amanhã. A CI semanal consulta novamente a base OSV. Ausência de alertas não comprova ausência de vulnerabilidades.

Não houve publicação de servidor na internet nem configuração de infraestrutura real. Certificados de produção, entrega SMTP real, permissões operacionais do MySQL, retenção de dados e monitoramento externo precisam ser validados no ambiente de destino. Não armazenar dados pessoais reais nos exemplos ou nos relatórios públicos.

## Executor de testes para a equipe

`testar.sh`, `testar.bat` e `testar.ps1` executam `scripts/Testar.java` com o JDK instalado. O comando compila, lê os resultados JUnit, testa servidor HTTP/H2 isolado com credenciais sintéticas e verifica backup/restauração com o CLI real. Os relatórios HTML/TXT ficam em `relatorios-testes/` (ignorado pelo Git). Bancos, contas e arquivos restaurados são removidos após a rodada; logs privados ficam em `.tools/testes`. O executor não lê as cinco contas particulares nem a configuração de banco/SMTP da instalação normal. Interrupção forçada pode deixar temporários sintéticos; veja [TESTAR.md](TESTAR.md). Os testes lógicos de expiração simulam datas, sem alterar o relógio do computador.

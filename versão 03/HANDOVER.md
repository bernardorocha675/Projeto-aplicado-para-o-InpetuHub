# Handover — InPETU Hub versão 03

Data: 15/10/2026. Base: versão 02 do commit `f000c23d1fae4f429c3106464fc876b0f9459cde`. Pacote Java: `3.0.0`.

Esta versão acrescenta logs técnicos persistentes. As pastas versão 01 e versão 02 e o README vazio da raiz foram preservados. Não adiciona os requisitos funcionais/visuais do PDF anterior.

## Iniciar e testar

Abra a pasta **versão 03** no VS Code. Instruções completas em [COMO-INICIAR.md](COMO-INICIAR.md).

| Sistema | Preparar uma vez | Iniciar | Testar com servidor parado |
|---|---|---|---|
| Linux/macOS | `bash configurar.sh` | `bash iniciar.sh` | `bash testar.sh` |
| Windows | `.\configurar.bat` | `.\iniciar.bat` | `.\testar.bat` |

Aguarde `event=SERVER_READY`, depois abra http://localhost:8080/. O configurador preserva arquivos privados existentes. Não existem senhas públicas padrão. Uma instalação nova sem pacote privado gera seu próprio administrador em `.secrets/accounts.properties`.

## O que mudou

- `observability/SafeLogEncoder.java`: JSON por linha, com horário UTC, nível, logger e identificação da requisição. Apenas a classe de eventos controlados escreve mensagens; mensagens livres de bibliotecas/exceções, argumentos, MDC adicional e dados estruturados arbitrários são omitidos. Exceções mantêm o tipo e até 20 pontos da pilha, sem mensagens ou causas textuais.
- `OperationalEvents.java`: eventos controlados de início/fim, método/status/duração HTTP e auditoria com IDs internos. Não recebe corpo, URI, query string, cookies, login, senha ou chave MFA.
- `RequestLogFilter.java`: identificador UUID criado pelo servidor e devolvido em `X-Request-ID`; limpeza do contexto ao terminar, inclusive em erro. Não aceita identificadores do cliente.
- `ServerLogEvents.java`: marca prontidão e encerramento do contexto.
- `PrivateLogDirectory.java`: prepara pasta privada antes do Spring; restringe POSIX/ACL e recusa links simbólicos. A proteção é por usuário do sistema operacional; administradores do computador continuam tendo poderes sobre os arquivos.
- `LogRetention.java`: limpeza serializada na prontidão e a cada minuto; remove apenas arquivos compactados reconhecidos e estáveis há pelo menos um minuto. Mantém as 14 datas UTC mais recentes (incluindo hoje) e aplica o limite aos arquivos antigos, preservando os mais recentes.
- `logback-spring.xml`: terminal e `logs/inpetu.log`; rotação diária UTC ou por 10 MB, gzip, histórico de 14 dias e limite de 200 MB de arquivos antigos. Limpeza é periódica; não é quota rígida. Arquivo ativo e escrita em andamento não entram nesse limite.
- `AuditService.java`: espelha apenas os campos controlados após commit. Rollback não gera falso sucesso; tentativas de autenticação em transação própria continuam registradas.
- `LoggingTest`, `AuditLogIntegrationTest` e `scripts/Testar.java`: verificações de confidencialidade, correlação, transação, rotação e execução real.
- `.github/workflows/security-v03.yml`: verificações próprias em seis plataformas. Dependabot acompanha o Maven da versão 03.

O banco de auditoria e sua tela administrativa continuam existindo em `/admin/config/auditoria`, para ADMIN com MFA. A tela mostra os últimos 200 eventos; isso não apaga os anteriores do banco. Não foi criado um visualizador público de arquivos de log.

## Atualizar uma instalação da versão 02

A estrutura do banco, as migrations e o formato do backup são compatíveis com a versão 02. O nome técnico `data/inpetu-v02.mv.db` foi mantido de propósito para restaurar backups existentes. Não renomeie o banco.

1. Pare o servidor da versão 02 com Ctrl+C. Faça backup cifrado usando `backup.sh`/`backup.bat`. Guarde a senha fora do repositório.
2. Teste primeiro uma restauração em uma pasta nova da versão 03. Disponibilize Java 17/21 por `JAVA_HOME` ou pelo sistema **antes** de configurar; o configurador criaria arquivos que o restore não sobrescreve. Pode reutilizar o JDK baixado na pasta `.tools` anterior. No macOS, JAVA_HOME aponta à pasta que contém `bin/java`, normalmente `Contents/Home`.
3. Na pasta nova, execute `bash backup.sh restore /caminho/privado/backup.inpetu` ou `.\backup.bat restore C:\caminho\backup.inpetu`. A senha é solicitada pelo terminal, nunca como argumento. Sem banco/configuração anteriores no destino.
4. Execute o configurador e depois inicie. O banco restaurado preserva usuários, senhas atuais, MFA, registros e o marcador de importação; não é preciso criar novas contas nem reimportar acessos. Mantenha a chave MFA restaurada.
5. Confira agenda, login, MFA e os novos logs. Depois de validar, faça a troca operacional. Não rode as versões 02 e 03 simultaneamente sobre o mesmo banco nem compartilhe o mesmo arquivo de log entre instâncias.
6. Para voltar à versão 02, pare a versão 03 e restaure o backup validado em uma instalação separada da versão 02. Evite perder alterações feitas depois do backup.

Os logs não entram no backup Java: ele continua incluindo banco, configuração e chave MFA, além do arquivo de contas quando presente. Para MySQL, use dump/restore apropriado. Migração a partir da versão 01 exige o procedimento de segurança documentado no handover da versão 02; não trate como atualização direta de 02 para 03.

## Segurança e operação

Leia [LOGS.md](LOGS.md) e [docs/SEGURANCA.md](docs/SEGURANCA.md). `logs/`, arquivos privados, bancos, certificados, backups, relatórios e ferramentas locais ficam fora do Git. As cinco contas privadas não foram copiadas para esta versão nem recriadas no código.

Não habilite logs HTTP/SQL/SMTP externos, flags de depuração que imprimam configurações ou outro encoder sem revisar sua confidencialidade. A política entregue mantém o root em WARN e os eventos controlados em INFO. Mensagens livres de frameworks aparecem como `FRAMEWORK_EVENT`; tipo de exceção e frames ajudam o desenvolvedor a localizar o problema sem gravar os valores. O banner ou avisos emitidos diretamente pela JVM não passam pelo encoder.

Não há coleta centralizada, alerta por email, painel de métricas nem trilha de arquivo inviolável. Quem controla o usuário do sistema pode ler/apagar os arquivos. A auditoria transacional no banco é a referência para operações confirmadas; arquivos são apoio operacional. Uma falha de disco/permissão em runtime pode impedir novas gravações e será sinalizada pelo Logback no terminal. Monitore espaço e permissões no ambiente de produção.

A autenticação, CSRF, MFA, limites, TLS e regras de negócio vêm da versão 02. Não houve deploy. As condições de SMTP/TLS/MySQL e cobertura de navegador continuam exigindo validação no ambiente de uso. Evidências desta entrega em [docs/VALIDACAO.md](docs/VALIDACAO.md).

## Correção de acesso e exibição de senha

A versão 03 possui botão Mostrar/Ocultar senha nos formulários de senha. Para login inválido, consulte [ACESSOS.md](ACESSOS.md): os comandos `acessos.sh`/`acessos.bat` conferem o arquivo privado com o banco local e permitem recuperação explícita, com servidor parado e confirmação após backup. Não apagam reservas/cadastros, não imprimem credenciais e preservam perfil/MFA de contas existentes. Reiniciar normalmente continua sem redefinir senhas.

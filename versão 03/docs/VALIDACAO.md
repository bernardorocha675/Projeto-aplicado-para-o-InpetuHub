# Validação da versão 03

Base: versão 02 no commit `f000c23d1fae4f429c3106464fc876b0f9459cde`. Evidências da versão 02 permanecem na pasta original; não são usadas como comprovação automática desta versão.

## Escopo desta rodada

O executor `testar.sh`/`testar.bat` executa os testes de regras/segurança existentes e os novos cenários de log: mensagens livres com dados sintéticos não são serializadas; JSON e exceções sem mensagens; identificação criada no servidor e limpeza de MDC após falha; campos livres recusados; rotação gzip sem perda das linhas; espelhamento transacional após commit e ausência em rollback.

O servidor real usa banco e contas sintéticas em diretório descartável. A etapa de logs verifica arquivo persistente, prontidão, HTTP, auditoria, requestId, proteção POSIX onde disponível e ausência dos dados privados enviados. Os demais testes HTTP/MFA/persistência/backup continuam sendo executados.

Resultados finais serão registrados após executar esta rodada. A CI própria está em `.github/workflows/security-v03.yml` e abrange Ubuntu x64/ARM64, macOS Intel/Apple Silicon e Windows x64/ARM64, além de segredos, dependências e CodeQL.

## Limites

Testes não substituem pentest nem verificação da infraestrutura de produção. Não houve deploy, uso de SMTP/certificado real nem testes gráficos em navegador. Rotação por tamanho é testada com limite menor para gerar arquivos compactados; não é necessário preencher 200 MB ou aguardar 14 dias. A retenção usa uma rotina serializada própria e é testada com relógio fixo, limites reduzidos e arquivos fictícios: exclui expirados/excedentes, preserva o ativo e a compactação recente. A rotação diária é simulada pelo relógio do Logback, sem alterar o computador. A limpeza automática de histórico do Logback foi desativada após o teste de rotações rápidas revelar remoção prematura de arquivos da data atual; a rotação e compactação continuam usando Logback. Disco cheio, permissões alteradas depois do início e encerramento abrupto exigem acompanhamento operacional.

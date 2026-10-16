# Validação da versão 0.3

## Login básico 0.3.2 — 16/10/2026

O carregamento das contas foi refeito: a migration V4 registra uma assinatura por entrada do arquivo privado. Contas novas ou alteradas são aplicadas ao iniciar, inclusive em banco já provisionado; entradas idênticas não desfazem senhas alteradas no painel. Usuários aceitam normalização de caixa, acentos e espaços. Senhas de acesso aceitam 8–64 caracteres no arquivo, no cadastro e na troca. Erros de senha não bloqueiam temporariamente o modo local; produção mantém sua limitação.

A rodada local final aprovou **86 verificações**, incluindo **67 casos JUnit**, sem falhas ou casos ignorados. Os cenários novos cobrem cinco contas copiadas tardiamente, importação incremental, preservação de contas/dados, pacote inválido sem alterações parciais, normalização do usuário, senhas de oito caracteres e repetição de tentativas locais. Um servidor H2 real confirmou a importação tardia sem apagar o banco; o restante do roteiro de regras, perfis, sessões, logs, backups e recuperação offline também passou.

As cinco credenciais reais da entrega foram aceitas por HTTP em banco novo e após atualizar um banco real de teste criado com o JAR 0.3.1fix. Antes da atualização, a rejeição dos cinco novos acessos por ignorar o arquivo tardio foi reproduzida no JAR anterior. Foram conferidos dez logins e dez verificações de perfil, sem imprimir valores nem alterar banco de uso. A conta sintética antiga e a chave da instalação foram preservadas.

O instalador privado Linux/macOS foi verificado com dados sintéticos para cópia, backup do arquivo anterior, caminhos com espaços/acentos e recusa de código antigo. O instalador PowerShell recebeu revisão de código; não houve execução local de PowerShell. As senhas e o pacote privado não integram o repositório, o ZIP público nem os artefatos da CI.

V1–V3 e as versões 0.1/0.2 foram preservadas. V4 acrescenta somente `importacao_conta`. A CI do commit da tag `v0.3.2` é a referência da rodada nativa em Linux, macOS e Windows; a publicação depende de sucesso dos testes e verificações. Permanecem as limitações anteriores de navegador, CodeQL e produção.

## Correção 0.3.1fix — 16/10/2026

O segundo fator foi removido do acesso, menu, rotas e redefinição administrativa. Login e senha abrem o painel para ADMIN e COLABORADOR, inclusive contas que já possuíam dados de MFA. As colunas antigas, as migrations, a chave privada e o formato de backup foram preservados. Não houve alteração nas versões 0.1/0.2, nas senhas ou nas regras de reservas.

A rodada local completa aprovou **78 verificações**, incluindo **60 casos JUnit**, sem falhas ou casos ignorados. As novas verificações cobrem acesso por senha com e sem dados antigos de MFA, remoção de GET/POST das rotas antigas, permissões em caminhos normais/codificados e expiração absoluta de sessão para ambos os perfis. HTTP real também confirmou o acesso sem autenticador e a remoção das rotas. O restante do roteiro de logs, persistência, revogação de sessão, backup/restauração e recuperação local de senhas passou.

Foram conferidos: migrations idênticas à base, ausência das credenciais privadas no conteúdo preparado, sintaxe shell/Python e links locais. O pacote público é conferido pelo publicador antes do envio. A CI do commit associado à tag `v0.3.1fix` é a referência da nova rodada em Linux, macOS e Windows; a publicação exige sucesso dos testes e verificações. A remoção do segundo fator reduz a proteção das contas administrativas. As limitações de CodeQL/navegador/produção descritas abaixo continuam válidas.

## Histórico anterior à correção

Os resultados abaixo pertencem aos commits citados e podem descrever segundo fator ainda ativo. Não são usados como comprovação automática da correção 0.3.1fix.

Base: versão 0.2 no commit `f000c23d1fae4f429c3106464fc876b0f9459cde`. Evidências da versão 0.2 permanecem na pasta original; não são usadas como comprovação automática desta versão.

## Escopo desta rodada

O executor `testar.sh`/`testar.bat` executa os testes de regras/segurança existentes e os novos cenários de log: mensagens livres com dados sintéticos não são serializadas; JSON e exceções sem mensagens; identificação criada no servidor e limpeza de MDC após falha; campos livres recusados; rotação gzip sem perda das linhas; espelhamento transacional após commit e ausência em rollback.

O servidor real usa banco e contas sintéticas em diretório descartável. A etapa de logs verifica arquivo persistente, prontidão, HTTP, auditoria, requestId, proteção POSIX onde disponível e ausência dos dados privados enviados. Os demais testes HTTP/MFA/persistência/backup continuam sendo executados.

## Resultado local concluído

- Java 17, compilação e pacote 0.3.0 concluídos.
- 55 casos JUnit aprovados, sem falhas ou testes ignorados.
- 72 verificações aprovadas no comando completo, incluindo HTTP real, autenticação, MFA, revogação de sessões, persistência H2, backup/restauração e logs.
- Log real contém eventos SERVER_READY, HTTP e AUDIT, e o requestId aparece tanto no cabeçalho de resposta quanto no arquivo.
- Login/senha sintéticos, chave MFA, query string e cabeçalhos sentinela não aparecem no log. As cinco credenciais reais não aparecem no conteúdo preparado para o Git nem no relatório.
- Pastas versão 0.1 e versão 0.2 têm os mesmos hashes da base; README da raiz segue vazio.

## Verificações no GitHub

Execução [38786754769](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/38786754769), código `0bf6c17395481af7fc6011b876fb6f8718d8e3b2`.

Gitleaks e exclusão de arquivos privados aprovados. Consulta OSV: 100 dependências de execução, zero alertas na consulta desta rodada. CodeQL security-extended retornou zero resultados no SARIF, mas registrou erro interno ao renderizar um caminho de `java/csrf-unprotected-request-type`; essa consulta não é evidência completa. Os testes de integração mantêm CSRF ativo e verificam a recusa de POST sem token.

Os nove jobs concluíram com sucesso: Ubuntu 24.04 x64/ARM64, macOS 15 Apple Silicon/Intel, Windows Server 2025 x64, Windows 11 ARM64, segredos, dependências e CodeQL. Cada plataforma executou o configurador e o roteiro completo. Os artefatos da CI incluem apenas relatórios HTML/TXT, sem bancos, contas, chaves ou logs privados. A publicação final acrescenta este registro de resultados; o código executável é idêntico ao da rodada aprovada. A CI própria está em `.github/workflows/security-v0.3.yml` e abrange Ubuntu x64/ARM64, macOS Intel/Apple Silicon e Windows x64/ARM64, além de segredos, dependências e CodeQL.

## Limites

Testes não substituem pentest nem verificação da infraestrutura de produção. Não houve deploy, uso de SMTP/certificado real nem testes gráficos em navegador. Rotação por tamanho é testada com limite menor para gerar arquivos compactados; não é necessário preencher 200 MB ou aguardar 14 dias. A retenção usa uma rotina serializada própria e é testada com relógio fixo, limites reduzidos e arquivos fictícios: exclui expirados/excedentes, preserva o ativo e a compactação recente. A rotação diária é simulada pelo relógio do Logback, sem alterar o computador. A limpeza automática de histórico do Logback foi desativada após o teste de rotações rápidas revelar remoção prematura de arquivos da data atual; a rotação e compactação continuam usando Logback. Disco cheio, permissões alteradas depois do início e encerramento abrupto exigem acompanhamento operacional.

## Atualização: recuperação de acesso e senha visível

Foi identificado no código o provisionamento único: contas copiadas após o primeiro início não entram no banco automaticamente. A mensagem da imagem é genérica; não comprova qual conta/arquivo existe no computador do colega. Não houve acesso àquela máquina.

A correção adiciona comandos offline H2 para conferir e recuperar contas a partir do arquivo privado, com lock de instalação, confirmação explícita, transação e auditoria. Testes novos cobrem conta ausente, senha diferente, reativação, preservação de perfil/MFA/contas não listadas e rollback de falha parcial. O executor testa a cópia tardia em um banco real, cancelamento e login da conta recuperada.

O botão Mostrar/Ocultar senha usa script local permitido pela CSP, não modifica o valor e volta a ocultar ao enviar/trocar de aba. Foi conferido por teste funcional JavaScript com DOM simulado e checagem de sintaxe, sem afirmar cobertura de navegador gráfico. A rodada local completa aprovou 76 verificações, incluindo 58 casos JUnit, e a recuperação via JAR real. As cinco credenciais originais também autenticaram por HTTP em um banco novo e isolado da versão 0.3, sem exibir valores, ativar MFA real ou alterar o banco de uso. Alterações e relatórios foram conferidos contra os valores privados: nenhum acesso real foi encontrado. A [execução 38792529766](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/38792529766), código `a73bcc421699db4f99a7971bd29ea76a0de500cc`, concluiu os nove jobs com sucesso: Ubuntu x64/ARM64, macOS Apple Silicon/Intel, Windows x64/ARM64, segredos, dependências e CodeQL. Cada plataforma executou o configurador e o roteiro completo, incluindo os modos de acesso via JAR. A ressalva do erro interno CodeQL/CSRF registrada acima permanece. A publicação final muda apenas esta evidência e o caminho sugerido para guardar o backup em pasta privada ignorada pelo Git; o código executável corresponde ao candidato testado.


## Padronização da numeração

As referências atuais usam as pastas `versão 0.1`, `versão 0.2` e `versão 0.3`, com pacotes Java `0.1.0`, `0.2.0` e `0.3.0`. Os registros de CI acima correspondem aos commits históricos citados, anteriores à renomeação; o resultado da nova rodada está no workflow do commit de publicação. Os nomes dos bancos e o conteúdo das migrations não foram alterados.

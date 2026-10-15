# Validação da versão 0.2

Data: 15/10/2026. Base original preservada: versão 0.1, commit 08f2de3.

## Rodada original de segurança, anterior ao executor

- Compilação Java 17 com Spring Boot 4.1.1.
- 46 testes passaram: 5 unitários de regras, 19 de fluxos, 2 de concorrência, 3 unitários de segurança e 17 de integração de segurança.
- Os novos testes verificam revogação das duas sessões antigas após mudanças de conta, MFA obrigatório para administrador, cifragem do segredo, repetição de TOTP, recuperação de uso único, restrição de colaboradores, limite de tentativas, preservação de CSRF/cabeçalhos, intake sem alterar CRM/cotas, conferência pela equipe e ausência de credenciais nos eventos.
- Concorrência foi validada na aprovação: mesmo horário não pode ser aprovado duas vezes e uma cota compartilhada não pode ser usada duas vezes.

## Verificações complementares

- Teste HTTP com servidor real: inicialização completa, agenda, login privado, encaminhamento ao segundo fator e persistência H2.
- Rotas com caracteres percentualmente codificados: o login continua limitado e uma sessão sem MFA não acessa a configuração administrativa.
- Backup H2 cifrado: conteúdo restaurado idêntico; senha incorreta e adulteração são recusadas antes da extração; arquivos existentes não são sobrescritos. O servidor em execução impede o backup pelo lock da instalação.
- Gitleaks 8.24.3 sobre uma exportação completa dos arquivos versionados: nenhum segredo encontrado. A chave pública RFC 6238 de teste tem uma exceção explícita, restrita a esse valor conhecido. Os arquivos locais privados não fazem parte da exportação.
- Consulta OSV de 100 dependências de execução resolvidas: nenhum alerta na consulta realizada. Tomcat 11.0.27 e Jackson 3.1.7 foram fixados após alertas nas versões transitivas anteriores.
- Revisão independente da fronteira de autenticação e do fluxo público: o desvio por caminhos codificados foi corrigido e coberto por testes.

- CodeQL `security-extended`: nenhum resultado de vulnerabilidade no SARIF da execução final. O analisador registrou erro interno ao renderizar um caminho da consulta `java/csrf-unprotected-request-type`; portanto, essa consulta não é evidência completa. CSRF permaneceu ativo e as requisições sem token foram rejeitadas nos testes de integração.
- As cinco contas entregues em privado foram autenticadas por HTTP real, conferindo o perfil de cada uma, sem registrar as credenciais no relatório.

## Compatibilidade nativa concluída

A [execução final da CI](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/38724503216) aprovou os nove jobs no código `2e2b386bc2b2edd279b0f621ac3e6d5cc2bd07fb`. Cada plataforma executou o configurador, os 46 testes, backup/restauração e o teste HTTP real. A repetição do macOS Intel foi necessária após uma resposta HTTP 502 do Maven Central; a nova tentativa passou.

| Sistema / runner | Arquitetura | Resultado |
|---|---|---|
| Ubuntu 24.04 | x64 | Aprovado |
| Ubuntu 24.04 ARM | ARM64 | Aprovado |
| macOS 15 Intel | x64 | Aprovado |
| macOS 15 | Apple Silicon / ARM64 | Aprovado |
| Windows Server 2025 | x64 | Aprovado |
| Windows 11 ARM | ARM64 | Aprovado |

Gitleaks, consulta OSV e CodeQL também concluíram com sucesso. Os limites específicos da análise CodeQL estão registrados acima. O commit de encerramento acrescenta somente documentação; o código executável é o mesmo da rodada aprovada. A CI está em `.github/workflows/security-v0.2.yml`.

## Limites

Testes automatizados não equivalem a um pentest exaustivo. Não houve deploy em produção nem uso de credenciais reais de SMTP ou certificado público. O programa de backup atende H2; MySQL precisa de ferramentas próprias. Os resultados da versão 0.1 não são reaproveitados como comprovação da versão 0.2.


## Atualização: comando de testes para a equipe

Foi acrescentado `testar.sh` / `testar.bat`, com instruções em [TESTAR.md](../TESTAR.md). A aplicação executável não recebeu mudanças de comportamento nesta atualização; as mudanças são no executor, testes, documentação e CI.

- 48 casos JUnit passaram. Foram acrescentados testes dos prazos absolutos de sessão/MFA e de expiração de solicitações/reservas.
- O comando completo aprovou 64 verificações locais, somando casos JUnit, compilação, HTTP real, persistência, backup e limpeza.
- O servidor real usou H2, chave MFA e contas sintéticas em uma instalação descartável. Foram verificados TOTP, códigos de recuperação, restrição de colaborador, CSRF, rotas codificadas, limite de login, troca de senha e revogação de outra sessão.
- O CLI real de backup foi testado com banco H2: restauração idêntica, senha errada, adulteração, sobrescrita e recusa com servidor aberto.
- Os arquivos privados da instalação normal foram comparados antes/depois e permaneceram idênticos. Nenhum dos cinco acessos particulares foi encontrado no HTML/TXT gerados.
- Três falhas simuladas do executor foram verificadas: compilação recusada, resultado JUnit antigo e teste ignorado. Todas produziram saída de erro, relatório sem falsa aprovação e impediram a etapa HTTP.
- Gitleaks sobre uma exportação dos arquivos versionados: nenhum segredo encontrado.

O roteiro manual e as limitações de cobertura dos 24 cenários do PDF estão em TESTAR.md. Testes HTTP não controlam um navegador gráfico nem comprovam entrega SMTP, autenticador do celular ou TLS/MySQL reais.


A [rodada nativa do executor](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/38754288116), no código `5b07db7ce1407c5d3e7d61a71e7fd2c469774fa8`, aprovou os nove jobs: seis plataformas (Ubuntu x64/ARM64, macOS Intel/Apple Silicon e Windows x64/ARM64), segredos, dependências e CodeQL. Cada plataforma executou a configuração e o novo comando de testes. Os relatórios HTML/TXT sem credenciais são guardados como artefatos da CI; bancos, chaves, contas e logs privados não são enviados como artefatos.


## Padronização da numeração

As referências atuais usam as pastas `versão 0.1`, `versão 0.2` e `versão 0.3`, com pacotes Java `0.1.0`, `0.2.0` e `0.3.0`. Os registros de CI acima correspondem aos commits históricos citados, anteriores à renomeação; o resultado da nova rodada está no workflow do commit de publicação. Os nomes dos bancos e o conteúdo das migrations não foram alterados.

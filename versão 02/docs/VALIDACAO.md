# Validação da versão 02

Data: 15/10/2026. Base original preservada: versão 01, commit 08f2de3.

## Evidência local já concluída

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

Gitleaks, consulta OSV e CodeQL também concluíram com sucesso. Os limites específicos da análise CodeQL estão registrados acima. O commit de encerramento acrescenta somente documentação; o código executável é o mesmo da rodada aprovada. A CI está em `.github/workflows/security-v02.yml`.

## Limites

Testes automatizados não equivalem a um pentest exaustivo. Não houve deploy em produção nem uso de credenciais reais de SMTP ou certificado público. O programa de backup atende H2; MySQL precisa de ferramentas próprias. Os resultados da versão 01 não são reaproveitados como comprovação da versão 02.

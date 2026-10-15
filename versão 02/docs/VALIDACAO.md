# Validação da versão 02

Data: 15/10/2026. Base original preservada: versão 01, commit 08f2de3.

## Evidência local já concluída

- Compilação Java 17 com Spring Boot 4.1.1.
- 46 testes passaram: 5 unitários de regras, 19 de fluxos, 2 de concorrência, 3 unitários de segurança e 17 de integração de segurança.
- Os novos testes verificam revogação das duas sessões antigas após mudanças de conta, MFA obrigatório para administrador, cifragem do segredo, repetição de TOTP, recuperação de uso único, restrição de colaboradores, limite de tentativas, preservação de CSRF/cabeçalhos, intake sem alterar CRM/cotas, conferência pela equipe e ausência de credenciais nos eventos.
- Concorrência foi validada na aprovação: mesmo horário não pode ser aprovado duas vezes e uma cota compartilhada não pode ser usada duas vezes.

## Verificações complementares

As verificações nativas, a consulta final de dependências, o teste HTTP, backup/restauração e a revisão independente serão registrados ao finalizar a entrega. A CI fica disponível em `.github/workflows/security-v02.yml` para reproduzir os resultados.

## Limites

Testes automatizados não equivalem a um pentest exaustivo. Não houve deploy em produção nem uso de credenciais reais de SMTP ou certificado público. O programa de backup atende H2; MySQL precisa de ferramentas próprias. Os resultados da versão 01 não são reaproveitados como comprovação da versão 02.

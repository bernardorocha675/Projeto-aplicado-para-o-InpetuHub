# Validação — 0.4.4

Primeira parte do relatório concluída: cadastro de espaços. [Release 0.4.4](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.4) publicada após aprovação dos 11 jobs no [run 39025880429](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39025880429). Tag e código testado: `fbe03611991e1ce2678eab93615594e3e90223b1`. Os 12 arquivos carregados foram conferidos, e as oito releases anteriores mantiveram seus IDs e arquivos, tamanhos e digests.

Validação local: passaram 91 testes Java (12 novos), 27 testes Go e go vet, cinco testes Node da agenda, dois do formulário administrativo e sete do receptor/coletor. O roteiro completo HTTP/perfis/contas/backup e o pacote Linux real com runtime privado passaram em dados descartáveis: quinze logins, reinício, atualização simulada, logs/versões e preservação de banco/chave/sessão.

Novas regressões: capacidade zero, negativa, decimal com ponto/vírgula, letras, vazio e estouro numérico; erro preserva campos/checkboxes; correção cria/edita registro sem perder o estado; edição inválida não altera o banco; log sanitizado registra rejeição; capacidade tem limites no HTML e valores financeiros continuam decimais; JavaScript permite reenviar.

A matriz passou em Ubuntu 24.04 x64/ARM64, macOS 15 Intel/Apple Silicon, Windows Server 2025 x64 e Windows 11 ARM64, com JDK 21. Também passaram código, segredos, dependências, CodeQL, relatório de CI e publicação. Inclui regras Java, iniciador, roteiro HTTP/backup e smoke do executável real. A validação local usou JDK 17 compatível com o alvo do POM.

Não há comprovação de todas as distribuições/edições ou navegadores, teste visual automatizado de SmartScreen/Gatekeeper ou implantação MySQL/SMTP/TLS. Mantém-se a limitação conhecida da interpretação de uma consulta CodeQL de CSRF. Os demais itens do PDF não foram corrigidos nesta parte.

Este registro final acompanha main depois da publicação. Os documentos dentro do ZIP de código são o instantâneo anterior à CI; a evidência final é o run da tag acima. A release imutável não foi alterada para atualizar documentos.

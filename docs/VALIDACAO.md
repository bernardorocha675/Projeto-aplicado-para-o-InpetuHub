# Validação — 0.4.3

A release 0.4.3 foi publicada após aprovação de todos os gates no [run 39020936011](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39020936011), no commit b06fcf9d89161a6c512d4e4f3c97c6c28f20ac54. Foram aprovados Ubuntu x64/ARM64, macOS Intel/Apple Silicon e Windows x64/ARM64, além de código, segredos, dependências, CodeQL e relatório de CI. Os 12 arquivos da release foram conferidos; as sete releases anteriores foram preservadas.

A matriz compila e testa Go/Java, executa o roteiro HTTP/backup e valida o executável junto do runtime privado: configuração sem arquivo de senhas, quinze autenticações (início, reinício e troca do iniciador), perfis, persistência de cadastro/banco/chave e encerramento. Os testes do atualizador usam um serviço fictício local, sem publicar releases falsas.

Consulte o run associado ao commit/tag v0.4.3 em Actions para o resultado concreto. Não há validação visual automatizada de Gatekeeper/SmartScreen, navegadores de todos os sistemas, quedas físicas de energia/disco ou migrações futuras. Não foi realizada implantação real em MySQL/SMTP/TLS.

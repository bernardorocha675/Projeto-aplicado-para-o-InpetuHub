# Validação — 0.4.3

A release somente é publicada pelo workflow da 0.4 depois que os seis ambientes nativos, varredura de segredos, consulta de dependências e CodeQL terminam com sucesso.

A matriz compila e testa Go/Java, executa o roteiro HTTP/backup e valida o executável junto do runtime privado: configuração sem arquivo de senhas, quinze autenticações (início, reinício e troca do iniciador), perfis, persistência de cadastro/banco/chave e encerramento. Os testes do atualizador usam um serviço fictício local, sem publicar releases falsas.

Consulte o run associado ao commit/tag v0.4.3 em Actions para o resultado concreto. Não há validação visual automatizada de Gatekeeper/SmartScreen, navegadores de todos os sistemas, quedas físicas de energia/disco ou migrações futuras. Não foi realizada implantação real em MySQL/SMTP/TLS.

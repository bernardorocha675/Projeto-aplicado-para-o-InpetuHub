# Validação — 0.4.4

Primeira parte do relatório: cadastro de espaços. Publicação ainda depende da conclusão dos testes e de todos os gates da matriz. Não usar o resultado da 0.4.3 como evidência desta correção.

Validação local: passaram 91 testes Java (12 novos), 27 testes Go e go vet, cinco testes Node da agenda, dois do formulário administrativo e sete do receptor/coletor. O roteiro completo HTTP/perfis/contas/backup e o pacote Linux real com runtime privado passaram em dados descartáveis: quinze logins, reinício, atualização simulada, logs/versões e preservação de banco/chave/sessão.

Novas regressões: capacidade zero, negativa, decimal com ponto/vírgula, letras, vazio e estouro numérico; erro preserva campos/checkboxes; correção cria/edita registro sem perder o estado; edição inválida não altera o banco; log sanitizado registra rejeição; capacidade tem limites no HTML e valores financeiros continuam decimais; JavaScript permite reenviar.

A matriz deve testar Ubuntu x64/ARM64, macOS Intel/Apple Silicon e Windows x64/ARM64, além de código, segredos, dependências, CodeQL, relatório de CI e publicação. Inclui regras Java, iniciador, roteiro HTTP/backup e smoke do executável real. Resultado final no run associado à tag.

Não há comprovação de todas as distribuições/edições ou navegadores, teste visual automatizado de SmartScreen/Gatekeeper ou implantação MySQL/SMTP/TLS. Mantém-se a limitação conhecida da interpretação de uma consulta CodeQL de CSRF. Os demais itens do PDF não foram corrigidos nesta parte.

# Registros automáticos — 0.4.3

Abrir o executável começa uma sessão automaticamente, sem botão de iniciar teste. Reabrir a janela de uma instância já em execução mantém a mesma sessão. Cada evento inclui data/hora, versão do aplicativo e versão do iniciador. Antes de selecionar o pacote, a versão do aplicativo aparece como unknown; a do iniciador já é conhecida.

São registrados configuração, atualização, início/parada, navegação, ações de negócio e erros. Uma troca automática de iniciador mantém a sessão e registra as versões anteriores e novas. O registro exclui senhas, cookies, tokens, emails, formulários e dados livres. Ações usam códigos e IDs locais. Erros incluem tipo e até 20 pontos do código, sem mensagens livres.

Ao usar Fechar iniciador, são gerados log.txt e relatorio.json em logs/sessoes/testeNN-data-id na pasta de dados do usuário. Fechar só a aba mantém a sessão ativa. Travamentos deixam os registros já gravados, recuperados como INTERROMPIDO na próxima abertura. Um desligamento abrupto pode perder os últimos bytes ainda não gravados pelo sistema operacional.

O envio começa automaticamente quando o destino estiver ativado em Testes/configuracao.json. A interface informa quando o envio aguarda configuração ou está pendente. O iniciador tenta novamente durante o uso e na próxima abertura. O time não precisa entrar no GitHub.

O Render recebe a cópia temporária e o GitHub Actions publica uma pasta numerada em Testes, com log.txt e resumo.json. O número global pode ser diferente do número local da sessão. O processamento normalmente ocorre a cada cinco minutos e pode atrasar. A cópia local permanece depois do envio; uma resposta 202 não significa publicação. Se a fila do Render for perdida, a cópia local é reenviada na próxima abertura.

Cada relatório público tem limite de 5.000 eventos/2 MiB. Qualquer omissão aparece no resumo. CONCLUIDO indica encerramento normal, não ausência de bugs: confira os eventos ERROR/WARN e a contagem de erros. Os logs operacionais privados continuam em logs/inpetu.log e logs/launcher-java.log.

Não apague os dados locais enquanto houver envios pendentes. Não compartilhe banco, chaves ou backups para relatar bugs. A equipe deve revisar os registros públicos e acompanhar armazenamento/disponibilidade. Detalhes do receptor e do coletor: [RECEPTOR.md](RECEPTOR.md) e [Testes/README.md](../Testes/README.md).

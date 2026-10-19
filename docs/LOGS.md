# Consultar os logs — 0.4.5

Os registros começam automaticamente ao abrir o executável. Não é necessário abrir VS Code nem iniciar um teste manual. Cada evento da sessão informa data/hora e as versões do aplicativo e do iniciador.

## Encontrar os arquivos

Na tela do iniciador, a área de ajuda informa o caminho do log local. A pasta de dados é `InPETU-Hub/0.4` dentro de LocalAppData no Windows, Library/Application Support no macOS e normalmente `~/.config` no Linux.

Abra essa pasta pelo gerenciador de arquivos. Em `logs/sessoes`, cada pasta `testeNN-data-id` corresponde a uma sessão. Depois de usar **Fechar iniciador**, abra `log.txt` num editor de texto. `relatorio.json` contém o mesmo diagnóstico estruturado. Fechar apenas a aba mantém a sessão aberta; travamentos são recuperados na próxima abertura.

Os logs operacionais privados ficam em `logs/inpetu.log` e `logs/launcher-java.log`. Eles ajudam o desenvolvedor quando há falha antes de iniciar, mas não devem ser publicados integralmente. Os relatórios públicos usam somente campos permitidos e são separados desses arquivos.

## Entender o resultado

| Informação | Significado |
| --- | --- |
| INFO | Acontecimento normal. |
| WARN | Atenção, recusa ou lentidão; não indica necessariamente bug. |
| ERROR | Falha que precisa ser investigada. |
| app / iniciador | Versões presentes naquele evento; antes da seleção do pacote, app pode aparecer como unknown. |
| CONCLUIDO | A sessão encerrou normalmente; ainda pode conter eventos ERROR. |
| INTERROMPIDO | A sessão anterior não encerrou normalmente e foi recuperada. |
| FALHA | A sessão ou verificação falhou. |

Os horários dos eventos usam UTC; para Brasília, normalmente subtraia três horas. O nome das pastas publicadas no GitHub usa o horário de São Paulo. Os números da sessão no computador e no GitHub podem ser diferentes.

## Relatar um bug

Anote a versão, sistema/arquitetura, horário, passos, resultado esperado e observado. Confira os eventos ERROR/WARN próximos da ação. Os relatórios usam códigos e IDs locais; tipos de exceção e pontos do código ajudam o desenvolvedor sem divulgar mensagens livres.

Se precisar correlacionar uma requisição, copie apenas o cabeçalho de resposta X-Request-ID na aba Rede do navegador. Não copie cookies, Authorization, formulário ou resposta completa. A auditoria administrativa continua disponível para a conta com permissão.

Revise os trechos antes de compartilhar. Não envie banco, chave, ambiente ou backups. Os logs locais têm rotação própria; os relatórios de sessão têm limites e informam eventos omitidos. Veja [RELATORIOS.md](RELATORIOS.md) para formatos, limites e envio.

O envio dos computadores depende do receptor ativado em `Testes/configuracao.json`. Registros de CI já publicados não significam que esse envio esteja ativo. A cópia local permanece até confirmação final no GitHub.

Na 0.4.5, REQUEST_REJECTED identifica recusa tratada e REQUEST_FAILED identifica falha inesperada. VALIDATION_REJECTED corresponde a formulário inválido; EMAIL_FAILED a falha de email. Use a referência de suporte mostrada na tela para localizar o mesmo requestId no log. REPORT_CONFIG_FAILED e REPORT_UPLOAD_FAILED indicam tentativa de envio pendente; REPORTS_CONFIRMED indica recibos finais recebidos. Uma resposta de fila não é confirmação final.

## Códigos adicionados na 0.4.11

ACTION_FAILED passa a trazer `result` com o motivo: DATA_IN_USE (outro processo usa os dados), ORPHAN_STILL_RUNNING (sistema anterior não encerrou), JAVA_CHECK_FAILED (o Java do pacote não executou a conferência), APP_NOT_STARTED, APP_START_TIMEOUT, PRIVATE_CONFIG_MISSING, NO_PACKAGE, NETWORK ou OTHER. ORPHAN_APP_FOUND e ORPHAN_APP_STOPPED indicam a recuperação de um sistema deixado aberto. UPDATE_SKIPPED informa que a atualização não foi instalada e a versão anterior continuou em uso; DAMAGED_VERSION_PRESERVED, que uma pasta de versão danificada foi renomeada. LAUNCHER_SIGNAL indica encerramento pelo sistema operacional. A saída da conferência do Java também fica em `logs/launcher-java.log`.

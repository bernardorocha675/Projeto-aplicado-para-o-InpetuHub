# Relatórios de execução e testes

Cada sessão do iniciador registra automaticamente ações, erros e as versões do aplicativo e do iniciador. Ao encerrar, o relatório entra na fila de envio. Sessões interrompidas são recuperadas na próxima abertura.

O receptor no Render recebe somente o formato de diagnóstico permitido. O GitHub Actions recolhe a fila e publica pastas como `teste01-2026-10-16_21-00-00-a1b2c3d4`, com `log.txt` e `resumo.json`. O número é global no repositório; o relatório também informa o número local da sessão. Os horários no nome da pasta usam America/Sao_Paulo e os eventos usam UTC.

O time não precisa entrar no GitHub. Nenhum token do GitHub fica no executável ou no receptor. A leitura da fila exige a identidade temporária do workflow oficial; somente o GitHub Actions tem permissão de gravar no repositório. O envio não é instantâneo: a coleta é agendada aproximadamente a cada cinco minutos e pode atrasar conforme a disponibilidade dos serviços.

Enquanto `configuracao.json` estiver com `enabled:false`, o registro local funciona e os relatórios aguardam configuração. Para ativar, o responsável deve publicar o receptor e informar a URL HTTPS exata terminada em `/v1/reports`; depois alterar `enabled` para `true`. Nunca colocar tokens nesse arquivo. Essa ativação não exige reinstalar o executável.

Os arquivos públicos não incluem senhas, tokens, formulários, mensagens livres de exceção, cookies, email, banco ou chaves. Ações de negócio usam IDs locais e o tipo da operação; os erros incluem tipo e pontos do código. Relatórios recebidos de computadores são diagnósticos enviados pelo cliente, sem atestado de identidade ou integridade do dispositivo.

Cada sessão publica até 5.000 eventos ou 2 MiB de registros estruturados. Se atingir o limite, o resumo informa quantos eventos foram omitidos. Os logs operacionais locais continuam sujeitos à rotação própria do aplicativo. O receptor limita tamanho, concorrência, fila e frequência de recebimento. Nunca apaga a fila local por uma simples resposta de recebimento: a confirmação final depende de o relatório existir no GitHub.

O plano gratuito do Render pode suspender o receptor quando ocioso; sua fila temporária também é perdida em reinícios/deploys. O relatório completo permanece no computador até a confirmação no GitHub e é reenviado durante o uso ou na próxima abertura. Não desinstale/apague os dados locais enquanto houver envios pendentes. Arquivos já publicados permanecem no histórico Git; revisão e arquivamento dos relatórios são responsabilidade da equipe.

O índice `recebidos.json` mantém a numeração e evita duplicatas. Não editar manualmente IDs, hashes ou números. Os relatórios de CI usam o mesmo mecanismo, com versão e conclusão dos ambientes testados.

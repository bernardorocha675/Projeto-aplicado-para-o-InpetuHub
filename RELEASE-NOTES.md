# InPETU Hub 0.3.3

- Atualização automática para o fix estável mais recente da linha 0.3.x ao iniciar.
- Conferência do pacote e dos arquivos, backup privado e recuperação de troca interrompida.
- Preservação do banco, contas, senhas, configurações, ferramentas e logs.

## Como ativar

Instale esta release uma vez na pasta **versão 0.3**, com o servidor parado e preservando `data/`, `.env.local` e `.secrets/`. Depois use `bash iniciar.sh` no Linux/macOS ou `.\iniciar.bat` no Windows. As próximas correções 0.3.x serão verificadas nesse comando, antes de abrir o sistema. Não passa para 0.4 nem instala rascunhos ou versões de teste.

Se a consulta ou o download falhar, usa a versão instalada, desde que as ferramentas e dependências já estejam disponíveis. Código local editado ou pacote inválido interrompem a atualização. Consulte **ATUALIZACOES.md** para instalação, funcionamento, desativação e retorno. Os backups automáticos são privados e não devem ser compartilhados.

Somente o mecanismo de atualização e seus testes/guias foram acrescentados. Login, senhas existentes, telas, regras e migrations permanecem iguais à 0.3.2.

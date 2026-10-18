# Atualizações e dados — 0.4

O iniciador verifica as releases oficiais ao abrir e novamente antes de iniciar. Seleciona o maior número estável **0.4.x**; não instala 0.5, rascunhos ou pré-releases. Confere o SHA-256 do ZIP, a plataforma, a versão e os arquivos principais antes de ativar uma instalação nova.

As versões são instaladas em pastas próprias no perfil do usuário. O EXE em execução não é substituído. Quando necessário, o iniciador abre o executável da versão nova antes de iniciar o aplicativo. Os arquivos da instalação, do Java e das configurações ficam fora das pastas de código-fonte.

O banco, chave e configurações são compartilhados apenas entre fixes da mesma linha 0.4. Antes de trocar uma versão instalada, o iniciador confere se o aplicativo está parado e guarda `data/`, `.secrets/` e os arquivos de ambiente em `backups/`. Versões anteriores permanecem disponíveis. A atualização é bloqueada enquanto o sistema está em uso.

Se a consulta ou download estiver indisponível, usa a versão completa já instalada. Um pacote corrompido não é instalado. O pacote ZIP completo também permite a primeira configuração offline; o iniciador avulso precisa de internet na primeira instalação.

Os backups automáticos contêm dados e chaves reais, não são cifrados e não devem ser publicados. Ficam na pasta de dados do usuário, com acesso restrito pelo perfil. Não há limpeza automática desses backups. Para retorno após falha, pare o aplicativo e preserve o estado atual; restaure o banco, chave e ambiente do mesmo backup em uma cópia separada com a versão anterior. Restaurar somente código antigo pode ser incompatível com um banco já migrado. A restauração perde alterações posteriores ao backup.

O mecanismo não garante reversão automática de migrations após falha de inicialização. A integridade depende de HTTPS, SHA-256 e da confiança no repositório oficial; os executáveis desta entrega não possuem certificado comercial nem notarização Apple.

## Configuração ausente e downloads interrompidos — 0.4.6

Se já existem dados, o iniciador não gera outra chave ou ambiente para substituir arquivos ausentes. Pare e preserve a instalação; recupere banco, `.secrets/mfa.key` e `.env.local` do mesmo backup. Não apague o banco nem gere uma chave para tentar recuperar o acesso. Arquivos privados que sejam atalhos ou diretórios também são recusados.

Se o corpo do download for interrompido, a versão instalada continua disponível e a pasta temporária é removida. A próxima abertura pode tentar novamente. Erros de integridade, de gravação local ou pacote inválido continuam interrompendo a atualização; nenhum pacote parcial é ativado.

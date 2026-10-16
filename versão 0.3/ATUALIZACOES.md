# Atualizações automáticas — linha 0.3

A partir da **0.3.3**, o comando normal de iniciar procura o fix mais recente publicado da linha **0.3.x**. Não muda para 0.4, não instala rascunhos nem versões de teste. A consulta acontece antes de abrir o servidor; não atualiza enquanto alguém está usando o sistema.

| Sistema | Iniciar e procurar atualizações |
|---|---|
| Linux/macOS | `bash iniciar.sh` |
| Windows | `.\iniciar.bat` |

## Instalar este recurso uma vez

Quem está na 0.3.2 ou anterior precisa instalar a **0.3.3 uma vez**, com o servidor parado. As versões antigas não têm o atualizador. Faça um backup, extraia o ZIP público da release e transfira o conteúdo da pasta `versão 0.3` para a mesma pasta da instalação, preservando `data/`, `.env.local` e `.secrets/`. Depois, use o comando normal de iniciar. Ferramentas já configuradas continuam disponíveis.

As contas e senhas existentes continuam iguais. O instalador privado antigo foi preparado para a 0.3.2: aplique-o antes da atualização ou, com o servidor parado, copie somente seu `ACESSOS-PRIVADOS/contas.properties` para `.secrets/accounts.properties`, guardando o arquivo anterior. O atualizador nunca baixa credenciais.

## O que acontece ao iniciar

1. Confere se esta instalação está parada e recupera uma eventual troca de código interrompida.
2. Consulta as releases do repositório oficial e escolhe o maior número 0.3.x, mesmo quando a última release geral pertence a outra linha.
3. Baixa o ZIP e seu resumo SHA-256 por HTTPS. Confere o checksum, a versão e o manifesto antes de substituir código.
4. Se houver alterações locais nos arquivos gerenciados, cancela a atualização. Não sobrescreve trabalho da equipe.
5. Guarda o código anterior e uma cópia de `data/`, `.secrets/`, `.env.local` e `.env`, quando existentes, em `backups/atualizacoes/<identificador>/`. Troca os arquivos públicos da versão, remove arquivos gerenciados que deixaram de existir e recompila a aplicação.

Dados, contas, senhas, chave da instalação, logs, ferramentas, relatórios e arquivos particulares não são substituídos. O manifesto gerencia somente os arquivos públicos listados da pasta `versão 0.3`; os documentos na raiz do repositório não são atualizados por esse mecanismo.

Se a consulta ou o download estiver indisponível, continua com a versão instalada. Para executar sem internet, Java, Maven e as dependências precisam já estar preparados e em cache. Um pacote recebido com integridade inválida interrompe a inicialização, sem instalar arquivos parcialmente verificados.

## Se aparecer um erro

Pare outras execuções desta cópia e repita o comando. Se editou o código, guarde suas alterações e compare-as com a release antes de atualizar manualmente. Nunca apague `data/` ou `.secrets/` para resolver um erro do atualizador.

É possível iniciar sem consultar novas releases:

```sh
# Linux/macOS — vale somente para este comando
INPETU_AUTO_UPDATE=0 bash iniciar.sh
```

```powershell
# Windows PowerShell — remova a variável depois de encerrar
$env:INPETU_AUTO_UPDATE = '0'
.\iniciar.bat
Remove-Item Env:INPETU_AUTO_UPDATE
```

Essa opção ainda verifica o lock e recupera uma troca interrompida. Não corrige uma instalação incompleta: se faltar arquivo ou ferramenta, use o pacote oficial e o guia de instalação.

Em uma cópia obtida por Git, a atualização troca os arquivos de trabalho e pode aparecer como alterações locais. Para desenvolver, desative a atualização automática e atualize pela rotina do repositório. O mecanismo não executa `git reset`, não apaga commits e não faz push.

## Backup e retorno

As cópias automáticas ficam somente no computador, com permissões restritas ao usuário. **Contêm dados e segredos reais, não são cifradas e não devem ser compartilhadas nem publicadas.** Não substituem o backup cifrado de `backup.sh`/`backup.bat`. Guarde-as até validar o fix e remova cópias antigas conscientemente; não há limpeza automática dessas cópias.

Uma interrupção durante a troca deixa um registro privado em `.tools/update-pending`. No próximo início, o código anterior é restaurado antes da consulta, inclusive com a atualização desativada. O banco não é alterado nessa recuperação. Se a recuperação falhar, preserve o registro e o backup para análise.

Uma falha posterior na compilação ou na aplicação **não desfaz automaticamente uma migration de banco**. Para retornar, pare o servidor, preserve também o estado atual e crie uma instalação separada com o conteúdo de `codigo/` do backup. Copie `privado/data/`, `privado/.secrets/` e os arquivos de ambiente daquele mesmo backup para essa cópia. Configure ferramentas nela, mantenha a atualização desativada e valide antes de substituir a instalação em uso. Restaurar só o código antigo pode ser incompatível com um banco já migrado; restaurar dados também perde alterações feitas depois do backup.

O backup automático cobre o H2 local dentro de `data/`. Não copia um MySQL externo. Para produção, mantenha a atualização automática desativada e use backup do banco e implantação acompanhada pelo responsável.

## Manutenção das releases

Atualize o número do pacote e os caminhos do JAR, conforme `RELEASES.md`. Depois de adicionar arquivos públicos novos ao Git, execute `python scripts/update_manifest.py` dentro da pasta da versão e inclua `scripts/update-files.sha256` no commit. `python scripts/update_manifest.py --check` confere a lista e os hashes; a publicação recusa manifesto desatualizado. Arquivos `.bat` são comparados com quebras de linha normalizadas para funcionar também em checkout Windows.

Cada fix precisa continuar compatível com scripts, Java e dados da linha 0.3. Preserve migrations já aplicadas e teste sobre cópias de bancos antigos. Uma release publicada não é substituída. O checksum detecta corrupção e depende da confiança na conta e na release oficial; não é uma assinatura independente.

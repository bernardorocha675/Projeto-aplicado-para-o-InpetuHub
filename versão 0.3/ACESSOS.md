# Entrar no sistema — versão 0.3.2

O acesso usa somente **usuário e senha**, sem aplicativo autenticador. Os novos acessos foram entregues em privado. Use as credenciais desta entrega; o ZIP público do GitHub não contém as cinco contas.

## 1. Atualizar o código

Pare o servidor com Ctrl+C e use a release **0.3.2**, na pasta **versão 0.3**. Ela contém `pom.xml`, `configurar.sh`, `configurar.bat`, `iniciar.sh` e `iniciar.bat`. Preserve `data/`, `.env.local` e `.secrets/`, incluindo `.secrets/mfa.key`. Não apague o banco.

## 2. Aplicar o pacote privado

Extraia o ZIP privado dentro de **versão 0.3**. Será criada a pasta visível `ACESSOS-PRIVADOS`.

- **Windows:** abra essa pasta e dê dois cliques em `APLICAR.bat`.
- **Linux/macOS:** no terminal da pasta versão 0.3, execute `bash ACESSOS-PRIVADOS/APLICAR.sh`.

Se o instalador não encontrar o projeto, informe o caminho da pasta que contém `pom.xml`. Ele recusa versões antigas, guarda uma cópia do arquivo anterior e copia as contas para `.secrets/accounts.properties`. Não substitui banco, chave privada ou configuração. Os scripts do pacote não iniciam nem atualizam o código automaticamente.

Se já tem apenas o arquivo de contas, é possível copiá-lo manualmente para `.secrets/accounts.properties`, preservando antes uma cópia privada do anterior. Use um arquivo UTF-8 válido com ao menos um administrador.

## 3. Iniciar e entrar

Se esta cópia ainda não foi configurada, execute `bash configurar.sh` em Linux/macOS ou `.\configurar.bat` no Windows. Depois execute `bash iniciar.sh` ou `.\iniciar.bat`. Espere o servidor ficar pronto e abra http://localhost:8080/login.

Digite o usuário e a senha recebidos. O usuário não diferencia maiúsculas, acentos ou espaços de digitação. A senha diferencia maiúsculas e minúsculas e deve ser copiada exatamente; o botão Mostrar senha ajuda a conferir. No modo local, erros de senha não bloqueiam temporariamente novas tentativas.

## Como as contas são carregadas

A partir da versão 0.3.2, o sistema importa contas novas ou alteradas ao iniciar, inclusive se o banco já foi usado. Isso corrige o caso em que o pacote era copiado depois da primeira execução e ficava ignorado. Não é preciso apagar dados ou executar recuperação manual para importar o pacote novo.

Uma entrada idêntica já aplicada não redefine senhas trocadas pelo painel nem reativa uma conta desativada pelo administrador. Alterar explicitamente os dados daquela conta no arquivo e reiniciar aplica a mudança somente a ela; contas não listadas e os registros de negócio são preservados em bancos já provisionados. Uma instalação antiga ainda não provisionada mantém a etapa inicial de desativação das contas legadas antes de importar as privadas.

## Se ainda não entrar

1. Confirme que o código é **0.3.2** e que o servidor foi reiniciado depois de aplicar o pacote.
2. Confira se existe `.secrets/accounts.properties` na mesma instalação que está executando o site. Cada computador pode ter um banco independente.
3. Use as novas credenciais. Se você mesmo trocou a senha pelo painel, use a senha atual.
4. Se um administrador desativou a conta, peça a ele para reativá-la.

Os comandos offline `acessos.sh verificar` / `acessos.bat verificar` continuam disponíveis para diagnóstico com o servidor parado. O modo `recuperar` continua sendo uma opção explícita para voltar às senhas do arquivo: faça backup antes, execute `acessos.sh recuperar` / `acessos.bat recuperar` e confirme `RECUPERAR COM BACKUP`. Ele não inicia o site, não imprime credenciais e mantém perfis/dados antigos de MFA de contas já existentes. Atende somente o H2 local.

O pacote contém senhas em texto para a configuração inicial. Guarde-o em privado; não envie os arquivos ou suas capturas de tela ao repositório.

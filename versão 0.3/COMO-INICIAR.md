# Como instalar e iniciar — versão 0.3

## 1. Abrir a pasta correta

Baixe o ZIP do GitHub e extraia tudo, ou clone:

```sh
git clone https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub.git
cd Projeto-aplicado-para-o-InpetuHub
cd "versão 0.3"
```

No VS Code, use Arquivo → Abrir Pasta → **versão 0.3**. Abra Terminal → Novo Terminal. O terminal precisa estar junto de `pom.xml`, `configurar.sh` e `configurar.bat`. Estar vendo a pasta no Explorer não define o diretório do terminal.

## 2. Preparar as contas privadas

Para os acessos da equipe, atualize para a release **0.3.2**, extraia o pacote privado **dentro da pasta versão 0.3** e aplique-o conforme [ACESSOS.md](ACESSOS.md). No Windows, abra `ACESSOS-PRIVADOS/APLICAR.bat`. Em Linux/macOS, execute `bash ACESSOS-PRIVADOS/APLICAR.sh` no terminal da pasta versão 0.3. O instalador guarda o arquivo de contas anterior e copia o novo para `.secrets/accounts.properties`, sem apagar o banco ou trocar a chave privada.

O ZIP público contém apenas o código. Sem pacote privado, o configurador gera um administrador aleatório para aquele computador, guardado no arquivo local de contas; ele não conhece as cinco contas entregues separadamente.

Ao iniciar a versão 0.3.2, contas novas ou alteradas no arquivo são carregadas mesmo se o banco já existia. Não apague o banco para corrigir login. Um pacote idêntico não desfaz uma senha trocada pelo painel. Não publique o pacote privado, suas senhas nem a pasta `.secrets`.

## 3. Linux/Ubuntu

```bash
pwd
ls pom.xml configurar.sh iniciar.sh
bash configurar.sh
```

Não execute o configurador com sudo. Se faltar alguma ferramenta no Ubuntu/Debian:

```bash
sudo apt update
sudo apt install -y curl tar ca-certificates coreutils
```

Repita o configurador e aguarde **Configuracao concluida**. Depois:

```bash
bash iniciar.sh
```

## 4. macOS Intel ou Apple Silicon

No Bash/Zsh do VS Code, confira a pasta com `pwd` e `ls pom.xml configurar.sh iniciar.sh`.

```sh
bash configurar.sh
bash iniciar.sh
```

Execute o segundo comando somente após o primeiro terminar. Java 17/21 compatível pode ser reutilizado; o configurador baixa as ferramentas quando necessário. Não precisa de Homebrew.

## 5. Windows

Selecione PowerShell ou Prompt de Comando no terminal do VS Code. A extensão PowerShell não é necessária.

```powershell
dir pom.xml
.\configurar.bat
.\iniciar.bat
```

Aguarde **Configuracao concluida** antes de iniciar. Não precisa de administrador nem alterar a política global de execução. Em Windows ARM64 o download automático usa Java 21; em x64 usa Java 17.

## 6. Acessar com login e senha

Aguarde o evento **event=SERVER_READY** no terminal. As linhas de log estão em formato JSON. Abra no mesmo computador:

- Agenda: http://localhost:8080/
- Login: http://localhost:8080/login

Use sua conta recebida em privado. Não há login público padrão. Se a instalação gerou uma conta própria, abra `.secrets/accounts.properties` apenas no seu computador para consultá-la.

Na release **0.3.2**, administradores e colaboradores entram somente com login e senha. O usuário aceita maiúsculas, acentos e espaços de digitação; a senha deve ser copiada exatamente. Não há aplicativo autenticador nem solicitação de código adicional.

## 7. Parar e usar novamente

Pressione Ctrl+C no terminal do servidor e aguarde encerrar. Para usar de novo, execute somente `bash iniciar.sh` ou `.\iniciar.bat` na pasta versão 0.3. A inicialização recompila o código sem repetir os testes.

O banco, as contas e os registros permanecem em `data/`. A chave privada da instalação está em `.secrets/mfa.key`; ela continua sendo usada em outras verificações. Não a regenere. Faça backup cifrado antes de atualizar ou migrar.

## 8. Problemas comuns

| Sintoma | Verificação |
|---|---|
| Arquivo ou diretório inexistente | Abra o terminal junto de pom.xml; extraia o ZIP inteiro. |
| Permissão negada em ./configurar.sh | Use `bash configurar.sh`. |
| Erro de download/checksum | Confira internet, proxy, certificados e acesso ao Adoptium, GitHub e Maven Central; repita. |
| Porta ocupada | Pare outra execução ou altere `PORT` em `.env.local` e use a nova porta. |
| Arquivo privado inválido | Não publique o conteúdo. Confira a estrutura do pacote privado e leia o erro sem incluir credenciais. |
| Sessão encerrada após edição da conta | Entre novamente; mudanças de conta revogam sessões por segurança. |
| Muitas tentativas | O modo local 0.3.2 não bloqueia por erros de senha. Se ainda aparecer esta mensagem, confira a versão executada; em produção, aguarde o prazo indicado. |
| Solicitação não aparece como ocupada | Somente uma reserva aprovada bloqueia horários. Confira a fila de solicitações e a identidade antes de aprovar. |
| Código por email não chegou | Confira SMTP, contato institucional e spam. Sem SMTP, o modo local usa conferência manual da equipe. |

Para compartilhar um erro, envie somente o comando e a mensagem, ocultando arquivos privados, credenciais, códigos e dados pessoais.

## 9. Conferir o funcionamento automaticamente

Pare o servidor com Ctrl+C e execute `bash testar.sh` (Linux/macOS) ou `.\testar.bat` (Windows). Abra `relatorios-testes/ultimo.html` no navegador. O comando usa banco e contas descartáveis e gera um relatório com aprovações, falhas e o que ainda precisa de conferência manual. Não precisa de Python. Leia [TESTAR.md](TESTAR.md).

## 10. Consultar os logs

Os arquivos ficam em `logs/inpetu.log`, dentro de versão 0.3. Abra esse arquivo no VS Code ou siga o guia [LOGS.md](LOGS.md). O código de identificação de uma requisição aparece no cabeçalho `X-Request-ID`. Os arquivos antigos são compactados automaticamente. Não envie a pasta inteira sem revisão.

Para trazer dados da versão 0.2 mantendo usuários, senhas e arquivos privados, siga o procedimento em [HANDOVER.md](HANDOVER.md). Instalações novas começam com banco vazio; a versão 0.3 não copia dados automaticamente.

## Correção de acesso e exibição de senha

A versão 0.3 possui botão Mostrar/Ocultar senha nos formulários de senha. Para login inválido, consulte [ACESSOS.md](ACESSOS.md): os comandos `acessos.sh`/`acessos.bat` conferem o arquivo privado com o banco local e permitem recuperação explícita, com servidor parado e confirmação após backup. Não apagam reservas/cadastros, não imprimem credenciais e preservam o perfil e os dados de contas existentes. Reiniciar normalmente continua sem redefinir senhas.

## Atualização automática a partir da 0.3.3

Instale a 0.3.3 uma vez para ter o atualizador. Depois, o comando normal de iniciar procura o fix mais recente da linha 0.3.x, faz backup e preserva contas, banco e configuração. Não passa para 0.4 automaticamente. Leia [ATUALIZACOES.md](ATUALIZACOES.md), inclusive para instalar o pacote privado antigo em uma cópia nova e para desativar a consulta durante desenvolvimento.

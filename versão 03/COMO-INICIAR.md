# Como instalar e iniciar — versão 03

## 1. Abrir a pasta correta

Baixe o ZIP do GitHub e extraia tudo, ou clone:

```sh
git clone https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub.git
cd Projeto-aplicado-para-o-InpetuHub
cd "versão 03"
```

No VS Code, use Arquivo → Abrir Pasta → **versão 03**. Abra Terminal → Novo Terminal. O terminal precisa estar junto de `pom.xml`, `configurar.sh` e `configurar.bat`. Estar vendo a pasta no Explorer não define o diretório do terminal.

## 2. Preparar as contas privadas

Se recebeu um pacote privado de acessos, extraia a pasta `.secrets` dele **dentro de versão 03**, antes de iniciar o sistema pela primeira vez. O resultado deve ser `.secrets/accounts.properties`. Não coloque o ZIP no repositório nem envie todas as contas a terceiros. O arquivo contém as senhas iniciais em texto: guarde-o em local privado, não em capturas de tela.

Sem esse arquivo, o configurador cria apenas uma conta administrativa aleatória para a instalação atual. O código não conhece as contas particulares de outras instalações. O configurador não imprime nenhuma credencial.

Se já iniciou este banco com outras contas, substituir o arquivo não troca usuários ou senhas. Para um primeiro teste sem dados importantes, use uma instalação nova; para preservar dados, siga a migração no handover. Nunca apague um banco que contenha registros necessários.

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

## 6. Acessar e configurar o segundo fator

Aguarde o evento **event=SERVER_READY** no terminal. As linhas de log estão em formato JSON. Abra no mesmo computador:

- Agenda: http://localhost:8080/
- Login: http://localhost:8080/login

Use sua conta recebida em privado. Não há login público padrão. Se a instalação gerou uma conta própria, abra `.secrets/accounts.properties` apenas no seu computador para consultá-la.

No primeiro acesso administrativo, configure um aplicativo autenticador: adicione uma conta por **chave manual**, selecione **TOTP**, seis dígitos e intervalo de 30 segundos. Digite o código gerado e guarde os oito códigos de recuperação, que só aparecem uma vez. Não publique a chave manual nem os códigos. Mantenha data/hora automáticas no telefone e computador.

Os colaboradores podem configurar o segundo fator pelo menu. Depois de ativado, ele será exigido nos próximos logins.

## 7. Parar e usar novamente

Pressione Ctrl+C no terminal do servidor e aguarde encerrar. Para usar de novo, execute somente `bash iniciar.sh` ou `.\iniciar.bat` na pasta versão 03. A inicialização recompila o código sem repetir os testes.

O banco, as contas e os registros permanecem em `data/`. A chave MFA está em `.secrets/mfa.key`; não a regenere. Faça backup cifrado antes de atualizar ou migrar.

## 8. Problemas comuns

| Sintoma | Verificação |
|---|---|
| Arquivo ou diretório inexistente | Abra o terminal junto de pom.xml; extraia o ZIP inteiro. |
| Permissão negada em ./configurar.sh | Use `bash configurar.sh`. |
| Erro de download/checksum | Confira internet, proxy, certificados e acesso ao Adoptium, GitHub e Maven Central; repita. |
| Porta ocupada | Pare outra execução ou altere `PORT` em `.env.local` e use a nova porta. |
| Arquivo privado inválido | Não publique o conteúdo. Confira a estrutura do pacote privado e leia o erro sem incluir credenciais. |
| Segundo fator inválido | Confira relógios, aguarde o próximo código ou use um código de recuperação ainda não utilizado. |
| Sessão encerrada após edição da conta | Entre novamente; mudanças de conta revogam sessões por segurança. |
| Muitas tentativas | Aguarde o prazo indicado. Não é um bloqueio permanente. |
| Solicitação não aparece como ocupada | Somente uma reserva aprovada bloqueia horários. Confira a fila de solicitações e a identidade antes de aprovar. |
| Código por email não chegou | Confira SMTP, contato institucional e spam. Sem SMTP, o modo local usa conferência manual da equipe. |

Para compartilhar um erro, envie somente o comando e a mensagem, ocultando arquivos privados, credenciais, códigos e dados pessoais.

## 9. Conferir o funcionamento automaticamente

Pare o servidor com Ctrl+C e execute `bash testar.sh` (Linux/macOS) ou `.\testar.bat` (Windows). Abra `relatorios-testes/ultimo.html` no navegador. O comando usa banco e contas descartáveis e gera um relatório com aprovações, falhas e o que ainda precisa de conferência manual. Não precisa de Python. Leia [TESTAR.md](TESTAR.md).

## 10. Consultar os logs

Os arquivos ficam em `logs/inpetu.log`, dentro de versão 03. Abra esse arquivo no VS Code ou siga o guia [LOGS.md](LOGS.md). O código de identificação de uma requisição aparece no cabeçalho `X-Request-ID`. Os arquivos antigos são compactados automaticamente. Não envie a pasta inteira sem revisão.

Para trazer dados da versão 02 mantendo usuários, senhas e MFA, siga o procedimento em [HANDOVER.md](HANDOVER.md). Instalações novas começam com banco vazio; a versão 03 não copia dados automaticamente.

## Correção de acesso e exibição de senha

A versão 03 possui botão Mostrar/Ocultar senha nos formulários de senha. Para login inválido, consulte [ACESSOS.md](ACESSOS.md): os comandos `acessos.sh`/`acessos.bat` conferem o arquivo privado com o banco local e permitem recuperação explícita, com servidor parado e confirmação após backup. Não apagam reservas/cadastros, não imprimem credenciais e preservam perfil/MFA de contas existentes. Reiniciar normalmente continua sem redefinir senhas.

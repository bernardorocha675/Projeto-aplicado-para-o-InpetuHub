# Como instalar e iniciar — versão 02

## 1. Abrir a pasta correta

Baixe o ZIP do GitHub e extraia tudo, ou clone:

```sh
git clone https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub.git
cd Projeto-aplicado-para-o-InpetuHub
cd "versão 02"
```

No VS Code, use Arquivo → Abrir Pasta → **versão 02**. Abra Terminal → Novo Terminal. O terminal precisa estar junto de `pom.xml`, `configurar.sh` e `configurar.bat`. Estar vendo a pasta no Explorer não define o diretório do terminal.

## 2. Preparar as contas privadas

Se recebeu um pacote privado de acessos, extraia a pasta `.secrets` dele **dentro de versão 02**, antes de iniciar o sistema pela primeira vez. O resultado deve ser `.secrets/accounts.properties`. Não coloque o ZIP no repositório nem envie todas as contas a terceiros. O arquivo contém as senhas iniciais em texto: guarde-o em local privado, não em capturas de tela.

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

Aguarde **Started InpetuApplication**. Abra no mesmo computador:

- Agenda: http://localhost:8080/
- Login: http://localhost:8080/login

Use sua conta recebida em privado. Não há login público padrão. Se a instalação gerou uma conta própria, abra `.secrets/accounts.properties` apenas no seu computador para consultá-la.

No primeiro acesso administrativo, configure um aplicativo autenticador: adicione uma conta por **chave manual**, selecione **TOTP**, seis dígitos e intervalo de 30 segundos. Digite o código gerado e guarde os oito códigos de recuperação, que só aparecem uma vez. Não publique a chave manual nem os códigos. Mantenha data/hora automáticas no telefone e computador.

Os colaboradores podem configurar o segundo fator pelo menu. Depois de ativado, ele será exigido nos próximos logins.

## 7. Parar e usar novamente

Pressione Ctrl+C no terminal do servidor e aguarde encerrar. Para usar de novo, execute somente `bash iniciar.sh` ou `.\iniciar.bat` na pasta versão 02. A inicialização recompila o código sem repetir os testes.

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

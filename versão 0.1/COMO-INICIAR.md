# Como instalar e iniciar o InPETU Hub

Este guia é para a versão guardada em `versão 0.1`. A instalação automática prepara Java, Maven e as dependências. O banco local é criado ao iniciar o aplicativo; não é necessário instalar MySQL para esse modo.

## 1. Baixar e abrir a pasta certa

Você pode baixar pelo GitHub em **Code → Download ZIP** e extrair tudo, ou clonar com Git:

```sh
git clone https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub.git
cd Projeto-aplicado-para-o-InpetuHub
cd "versão 0.1"
```

No VS Code, escolha **Arquivo → Abrir Pasta** e abra **`versão 0.1`**. Ela contém `pom.xml`, `configurar.sh`, `configurar.bat`, `iniciar.sh` e `iniciar.bat`.

Abra **Terminal → Novo Terminal**. O terminal precisa estar nessa pasta. Se abriu a raiz do repositório no VS Code, entre na versão pelo terminal:

```sh
cd "versão 0.1"
```

Execute esse `cd` somente se ainda estiver na raiz. Outra opção é clicar com o botão direito em **`versão 0.1`** no Explorer do VS Code e selecionar **Open in Integrated Terminal / Abrir no Terminal Integrado**.

**Ver os arquivos no painel esquerdo não significa que o terminal está na mesma pasta.** Esse foi o motivo do erro de caminho observado no Ubuntu.

## 2. Linux — incluindo Ubuntu

### Conferir a pasta

No terminal Bash:

```bash
pwd
ls pom.xml configurar.sh iniciar.sh
```

Os três arquivos devem ser listados. Se algum não for encontrado, abra o terminal na pasta `versão 0.1` antes de continuar.

### Instalar e configurar

```bash
bash configurar.sh
```

Não use `sudo` para executar o configurador. Ele usa a pasta do projeto e não precisa instalar Java ou Maven globalmente.

Se o configurador informar que falta `curl`, `tar` ou uma ferramenta de checksum, no Ubuntu/Debian prepare os utilitários:

```bash
sudo apt update
sudo apt install -y curl tar ca-certificates coreutils
```

Depois, repita `bash configurar.sh`. Em outras distribuições, use o gerenciador de pacotes correspondente para obter esses utilitários.

Espere a configuração terminar com **`Configuracao concluida`**. A primeira execução precisa de internet e pode demorar por causa dos downloads e testes.

### Iniciar

```bash
bash iniciar.sh
```

Mantenha esse terminal aberto enquanto estiver usando o aplicativo. Continue no passo 5.

## 3. macOS — Intel ou Apple Silicon

Use o terminal integrado do VS Code, Bash ou Zsh. Confira a pasta:

```sh
pwd
ls pom.xml configurar.sh iniciar.sh
```

Configure uma vez:

```sh
bash configurar.sh
```

O script usa `curl`, `tar` e `shasum` do sistema. Reutiliza um JDK 17 ou 21 compatível ou baixa Temurin 17 para a arquitetura detectada. O Maven também é preparado automaticamente. Não é necessário instalar Homebrew para executar esses comandos.

Espere **`Configuracao concluida`**. Depois inicie:

```sh
bash iniciar.sh
```

Mantenha o terminal aberto e continue no passo 5.

## 4. Windows

No VS Code, abra **Terminal → Novo Terminal** e selecione **PowerShell** ou **Prompt de Comando**. A extensão PowerShell do VS Code não é necessária para os comandos deste guia.

Confira os arquivos no terminal:

```powershell
dir pom.xml
dir configurar.bat
dir iniciar.bat
```

Se algum não for encontrado, abra o terminal dentro de `versão 0.1`.

Configure uma vez:

```powershell
.\configurar.bat
```

O arquivo `.bat` chama o PowerShell e prepara Java, Maven e as dependências. Não precisa abrir como administrador nem mudar a política de execução global do Windows.

Espere **`Configuracao concluida`**. Depois inicie:

```powershell
.\iniciar.bat
```

Mantenha esse terminal aberto enquanto usa o aplicativo.

## 5. Abrir o sistema e entrar

Depois do comando de iniciar, aguarde a mensagem **`Started InpetuApplication`** no terminal. Se aparecer erro, o servidor pode não ter iniciado.

Abra no navegador **do mesmo computador**:

- Agenda pública: <http://localhost:8080/>.
- Login do CRM: <http://localhost:8080/login>.

Em um banco novo, a conta inicial de desenvolvimento é:

| Campo | Valor |
|---|---|
| Login | `admin@inpetuhub.local` |
| Senha | `Inpetu@123` |

Se o banco já existia e a conta foi alterada, use a credencial atual. Mudar `ADMIN_PASSWORD` no arquivo de configuração não redefine uma senha já cadastrada.

O modo automático funciona sem configurar email. Para enviar emails reais, configure as credenciais conforme o [README da versão](README.md).

## 6. Parar e abrir novamente

Para parar, vá ao terminal do servidor e pressione **Ctrl+C**. Aguarde a finalização antes de abrir outra execução.

Para usar de novo, abra um terminal na pasta `versão 0.1` e execute somente:

| Sistema | Comando |
|---|---|
| Linux ou macOS | `bash iniciar.sh` |
| Windows | `.\iniciar.bat` |

Não precisa repetir o configurador toda vez. O comando de iniciar faz uma compilação limpa para incorporar mudanças no código e inicia o aplicativo.

Os registros ficam em **`data/inpetu.mv.db`**. Não apague `data/` ao atualizar o código. Para fazer backup, pare o sistema e copie `data/` e `.env.local` para um local privado.

## 7. Resolver problemas comuns

| Sintoma | O que conferir |
|---|---|
| `Arquivo ou diretório inexistente` ao chamar o script | O terminal deve estar em `versão 0.1`, junto de `pom.xml`. Confira com `pwd`/`ls` no Linux/macOS ou `Get-Location`/`dir` no PowerShell. Extraia o ZIP inteiro se faltarem arquivos. |
| `.bat` não funciona no Linux/macOS | Use `bash configurar.sh` e `bash iniciar.sh` nesses sistemas. |
| Permissão negada ao usar `./configurar.sh` | Execute `bash configurar.sh`, como neste guia. |
| O script pede para configurar primeiro | Execute o configurador e espere a mensagem de conclusão; se falhar, resolva o erro antes de iniciar. |
| Download ou checksum falhou | Confira internet/proxy e acesso a `api.adoptium.net`, GitHub e `repo.maven.apache.org`. Repita o configurador; guarde a mensagem completa se persistir. |
| Porta 8080 já está em uso | Pare a outra execução ou altere `PORT=8081` em `.env.local`, salve e inicie novamente. Depois abra `http://localhost:8081`. |
| Banco H2 já está em uso | Pare a execução anterior antes de iniciar outra na mesma pasta. |
| `java` ou `mvn` não aparece no PATH | Use os comandos deste guia, que procuram as ferramentas locais; não é preciso chamar `java` ou `mvn` diretamente. |
| O navegador não abre o sistema | Confira se o servidor iniciou, se o terminal continua aberto e se a porta do endereço corresponde a `PORT`. |

Se precisar de ajuda, envie **o comando executado e a mensagem completa do erro**, incluindo o nome do arquivo citado.

## 8. Testes e informações técnicas

Depois de configurar, rode os testes quando precisar verificar uma alteração:

```sh
# Linux/macOS
bash maven.sh test
```

```powershell
# Windows
.\maven.bat test
```

O configurador já executa a suíte completa. O comando de iniciar compila sem executar os testes novamente.

Esta versão passou pela instalação, pelos 26 testes e pela inicialização nativa em Linux, macOS e Windows, em x64 e ARM64. Quando precisa baixar Java no Windows ARM64, o configurador usa Temurin 21; no Windows x64 usa Temurin 17. Consulte [docs/VALIDACAO.md](docs/VALIDACAO.md) para os sistemas testados e os limites da verificação.

Para usar MySQL, veja [README.md](README.md). Para arquitetura, manutenção e pendências, veja [HANDOVER.md](HANDOVER.md).

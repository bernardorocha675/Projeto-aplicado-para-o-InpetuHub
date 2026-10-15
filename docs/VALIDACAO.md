# Validação desta atualização

Verificação realizada em 14 de outubro de 2026, horário de Brasília.

## Instalação e execução

- Ambiente de execução: Linux x64, Temurin JDK 17.0.20.1 e Maven 3.9.9.
- Download do JDK a partir da API oficial Adoptium e verificação SHA-256: sucesso.
- Download do Maven e verificação SHA-512: sucesso.
- Configuração com `bash configurar.sh`, compilação e empacotamento: sucesso.
- 26 testes do projeto passaram: 5 unitários, 19 de integração e 2 de concorrência. Sem falhas, erros ou testes ignorados.
- Inicialização pelo comando `bash iniciar.sh`, executado a partir de outra pasta: sucesso.
- Perfil `local` ativo, migrations Flyway aplicadas e banco H2 em arquivo criado em `data/`.
- A porta foi alterada para 18080 em `.env.local` e respeitada na execução.
- Agenda pública, API de disponibilidade, formulário de login e dashboard responderam via HTTP real.
- Login de administrador com cookies de sessão e token CSRF: sucesso.
- Um feriado de teste foi gravado pelo formulário administrativo e consultado após reiniciar.
- O configurador foi executado novamente: hashes de `.env.local` e do banco permaneceram iguais, sem sobrescrever configurações ou dados.
- Uma nova inicialização recompilou o pacote e manteve o registro gravado no banco.
- Sintaxe Bash, PowerShell, JavaScript e JSON do VS Code: válida.

A inicialização usa uma compilação limpa para evitar reaproveitar um JAR antigo
ou classes removidas. Os testes usam H2 em memória; o aplicativo no perfil local
usa H2 em arquivo. O ZIP entregue contém código e exemplos de configuração,
sem ferramentas baixadas, dependências, banco de teste ou configurações geradas.

## Limites da verificação

Windows e macOS não foram executados nativamente nesta atualização. Os scripts
PowerShell passaram pelo parser do PowerShell 7; isso não substitui um teste no
Windows PowerShell 5.1. A lógica para macOS inclui JDK Intel e Apple Silicon,
mas a execução real disponível aqui foi Linux x64.

Não houve teste visual em navegador nesta atualização. As páginas foram renderizadas
pelo servidor e verificadas por HTTP. O envio real por Gmail e a execução em MySQL
não foram repetidos. A configuração MySQL e as migrations originais foram preservadas.
Para usar Gmail, ainda é necessário fornecer credenciais próprias.

## Reproduzir

Na raiz do projeto, no macOS/Linux:

```sh
bash configurar.sh
bash iniciar.sh
```

No Windows:

```powershell
.\configurar.bat
.\iniciar.bat
```

Para rodar apenas os testes depois da configuração:

```sh
bash maven.sh test
```

No Windows, use `.\maven.bat test`.

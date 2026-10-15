# Validação desta atualização

Verificação realizada em 14 de outubro de 2026, horário de Brasília.

## Verificação inicial no ambiente local

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

A verificação inicial do ZIP foi realizada em Linux x64. A etapa adicional abaixo
executou instalação, testes e inicialização em runners nativos Linux, macOS e Windows.

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

## Verificação adicional de compatibilidade — 14/10/2026

As verificações nativas foram executadas pelo GitHub Actions com a pasta
`versão 01`, incluindo espaço e acento no caminho. Cada ambiente executou o
configurador completo, os 26 testes e o comando de iniciar correspondente.

| Sistema do runner | Arquitetura | Testes | Agenda, login, API e H2 | Evidência |
|---|---|---|---|---|
| Ubuntu 24.04 | x64 | 26, sem falhas | Aprovados | [Execução](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/38660131241/job/116179168374) |
| Ubuntu 24.04 | ARM64 | 26, sem falhas | Aprovados | [Execução](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/38660131241/job/116179168459) |
| macOS 15 | Intel x64 | 26, sem falhas | Aprovados | [Execução](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/38660131241/job/116179168368) |
| macOS 15 | Apple Silicon ARM64 | 26, sem falhas | Aprovados | [Execução](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/38660131241/job/116179168129) |
| Windows Server 2025 / PowerShell 5.1 | x64 | 26, sem falhas | Aprovados | [Execução](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/38660330751/job/116179814532) |
| Windows 11 / PowerShell 5.1 | ARM64 | 26, sem falhas | Aprovados | [Execução](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/38660330751/job/116179814679) |

O teste HTTP abriu a agenda, obteve o token CSRF, autenticou o administrador,
acessou o dashboard, consultou a API de disponibilidade e confirmou a criação
de `data/inpetu.mv.db`. Não foi um teste visual de navegador.

### Correções necessárias

1. **Windows ARM64:** a API do Temurin 17 não retornou pacote para essa plataforma.
   O configurador passou a baixar Temurin 21 em ARM64. Windows x64 continua
   baixando Temurin 17, e instalações existentes de Java 17/21 continuam aceitas.
2. **Launchers do Windows:** ao chamar Windows PowerShell 5.1 a partir de
   PowerShell 7, o `PSModulePath` herdado impediu carregar `Get-FileHash` nos
   runners. Os três `.bat` agora limpam essa variável dentro de `setlocal`,
   antes de abrir o PowerShell do sistema. A mudança não altera o ambiente
   do terminal que chamou o script. Os testes nativos passaram após o ajuste.

O código Java, os templates, as migrations, os testes e os scripts Linux/macOS
não precisaram de alterações. O JDK 21 Windows ARM64 também foi baixado,
conferido por SHA-256 e extraído em teste adicional; o executável foi identificado
como ARM64. A aplicação e os 26 testes passaram em Linux com Java 17 e com Java 21.

O arquivo de workflow foi temporário para esta verificação e removido ao final.
Os links acima apontam para os registros das execuções, sujeitos à retenção do GitHub.
O README da raiz continua vazio.

### Alcance

Os resultados se referem às seis combinações de sistema/arquitetura da tabela.
Não comprovam compatibilidade com todas as versões antigas dos sistemas, todas
as distribuições Linux ou ambientes de 32 bits. MySQL real, envio SMTP real e
inspeção visual da interface não fizeram parte desta verificação adicional.

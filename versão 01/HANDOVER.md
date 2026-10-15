# Handover — InPETU Hub

**Referência:** versão `1.0.0` do `pom.xml`, preparada para execução local e apresentação.
**Data:** 14 de outubro de 2026.
**Repositório:** [bernardorocha675/Projeto-aplicado-para-o-InpetuHub](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub).

## 1. Origem e escopo da entrega

Esta entrega parte do arquivo `InPETU-Hub-VSCode(1)(1).zip`, com 109 arquivos.
SHA-256 do ZIP de origem:

```text
c62837557ea50400d2bfadc0554aeb2931b107aa600591b0d3847306102ce909
```

O conteúdo da pasta `inpetu-hub` do ZIP está na pasta `versão 01` do
repositório. Dentro dela, `pom.xml`, `configurar.sh` e `iniciar.sh` ficam no mesmo nível.
O README na raiz do repositório está vazio; a documentação desta versão fica
junto do código em `versão 01`.
As classes Java, as migrations SQL, os testes, os templates e os scripts de
execução foram preservados. Foram acrescentados este handover e instruções de
clonagem no README; o exemplo SMTP do README usa um endereço genérico.

O projeto oferece agenda pública de espaços e um CRM interno para administrar
solicitações, empreendedores, atendimentos, visitas, preços, cotas e parâmetros.
A publicação do código no GitHub não inicia um servidor nem publica o sistema
na internet.

## 2. Retomar o projeto em outro computador

Clone o repositório e abra a pasta `versão 01` no VS Code:

```sh
git clone https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub.git
cd Projeto-aplicado-para-o-InpetuHub
cd "versão 01"
```

O terminal precisa estar na pasta que contém `pom.xml`.

| Sistema | Instalar e configurar | Iniciar |
|---|---|---|
| Ubuntu/Linux ou macOS | `bash configurar.sh` | `bash iniciar.sh` |
| Windows, PowerShell ou CMD | `.\configurar.bat` | `.\iniciar.bat` |

Execute o configurador e espere `Configuracao concluida` antes de iniciar.
A primeira configuração precisa de internet: o download do JDK pode ter cerca
de 200 MB. O configurador reutiliza um JDK 17 ou 21 com compilador, ou baixa
Temurin 17; prepara Maven 3.9.9 em `.tools/`, verifica checksums, cria
`.env.local` se necessário e executa `clean verify`.

O comando de iniciar usa `clean package` com testes ignorados nessa etapa e
inicia o JAR com o perfil `local`. Essa compilação limpa incorpora alterações
e evita reaproveitar classes removidas ou um JAR antigo. Os testes completos
já fazem parte do comando de configuração e podem ser executados separadamente.

- Agenda: <http://localhost:8080/>.
- Login do CRM: <http://localhost:8080/login>.
- Conta inicial de demonstração: `admin@inpetuhub.local` / `Inpetu@123`.
- Aguarde `Started InpetuApplication`; para parar, pressione `Ctrl+C`.
- Se `PORT` for alterada, abra a nova porta no navegador.

A conta de demonstração é destinada ao desenvolvimento. `ADMIN_LOGIN` e
`ADMIN_PASSWORD` só definem a criação inicial; mudar essas variáveis não
redefine a senha de um administrador que já existe no banco. Contas podem
ser editadas em Administração → Colaboradores.

### Erro observado no Ubuntu

Nos prints analisados, o terminal estava na pasta pai `Projeto Aplicado`,
enquanto os scripts estavam numa subpasta do projeto. Por isso o Bash retornou
`Arquivo ou diretório inexistente` antes de executar o configurador.

Abra a pasta que contém o `pom.xml` e use o terminal dessa pasta. Também é
possível clicar com o botão direito nela no Explorer do VS Code e escolher
**Open in Integrated Terminal**. Os scripts `.sh` são usados no Ubuntu;
instalar a extensão PowerShell não corrige o diretório atual do terminal.
O configurador não exige `sudo`.

Se faltarem ferramentas básicas no Ubuntu, instale-as antes de repetir:

```sh
sudo apt update
sudo apt install -y curl tar ca-certificates coreutils
```

## 3. Bancos, configuração e dados locais

| Uso | Configuração | Persistência |
|---|---|---|
| Execução automática | Perfil `local`, `application-local.properties` e `.env.local` | H2 em `data/inpetu.mv.db` |
| MySQL | Perfil padrão, `application.properties` e `.env` | Banco MySQL configurado pelo responsável |
| Testes | Perfil `test` | H2 em memória, isolado do banco local |

No perfil local, o servidor escuta em `127.0.0.1`. O H2 não depende de instalar
um serviço separado. Fechar e abrir mantém os registros. O modo de compatibilidade
MySQL do H2 não transforma os dois motores no mesmo banco; os dados de um
MySQL anterior não são importados automaticamente.

Para usar MySQL: instale e inicie o servidor, ajuste a senha e execute
`sql/criar-banco.sql`; configure `.env` com base em `.env.example`. Depois da
configuração de Java/Maven, rode `bash maven.sh spring-boot:run` ou
`.\maven.bat spring-boot:run`. Os scripts `iniciar` sempre selecionam H2.

| Variável | Função |
|---|---|
| `PORT` | Porta HTTP; padrão 8080 |
| `ADMIN_LOGIN`, `ADMIN_PASSWORD` | Criação inicial do administrador |
| `SEED_DEMO` | Habilita exemplos; `false` não impede criar o administrador |
| `LOCAL_DB_PASSWORD` | Senha opcional do H2; definir antes de criar o banco |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexão MySQL |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Credenciais opcionais de SMTP |

Os arquivos de configuração seguem o formato Java properties: valores sem
aspas e barras invertidas devidamente escapadas. Variáveis do ambiente têm
precedência. Sem credenciais SMTP, a aplicação funciona e registra o envio
como não configurado.

`.env`, `.env.local`, `.tools/`, `target/`, `data/` e logs são ignorados pelo Git.
Os exemplos `.env.example` e `.env.local.example` acompanham o código.
Para backup do modo H2, pare o aplicativo e copie `data/` e `.env.local` para
um local privado. Não apague `data/` para atualizar o código. Um `git clone`
transfere código e documentação, não os registros cadastrados em outro PC.

## 4. Onde cada parte está implementada

Stack: Java 17, Spring Boot 3.5.6, Maven, MVC/Thymeleaf, Spring Security,
JPA/Hibernate, Bean Validation, Flyway e Mail. HTML, CSS e JavaScript ficam
no próprio projeto; não há build de frontend com Node.

| Local | Responsabilidade |
|---|---|
| `src/main/java/br/com/inpetu/hub/controller/` | Agenda, formulários públicos, páginas administrativas e CRUD |
| `service/ReservaService.java` | Solicitar, aprovar, recusar, alterar, cancelar, concluir e registrar pagamentos |
| `service/DisponibilidadeService.java` | Horários, capacidade, disponibilidade e conflitos |
| `service/PrecoService.java` e `TaxacaoService.java` | Orçamento, percentuais, sinal e composição de valores |
| `service/CotaService.java` | Cotas mensais e movimentações de horas |
| `service/CrudService.java` | Cadastros e validações administrativas |
| `service/EmailService.java` | Notificações após o commit da transação |
| `entity/` e `repository/` | Entidades e acesso JPA ao banco |
| `security/SecurityConfig.java` | Login, perfis, proteção de rotas e CSRF |
| `config/DemoInitializer.java` | Administrador e dados fictícios de demonstração |
| `config/ClockConfig.java` | Relógio com fuso `America/Sao_Paulo` |
| `src/main/resources/templates/` | Páginas Thymeleaf públicas, CRM e erros |
| `src/main/resources/static/` | CSS e JavaScript da interface |
| `src/main/resources/db/migration/` | Estrutura SQL e dados iniciais versionados |
| `src/test/` | Testes unitários, integração, segurança e concorrência |
| `.vscode/` | Tarefas, recomendações de extensão e depuração |

As linhas abreviadas `service/`, `entity/`, `repository/`, `security/` e `config/`
na tabela são relativas a `src/main/java/br/com/inpetu/hub/`.

A migration V1 cria as tabelas e restrições; V2 cadastra personas, preços de
2026, parâmetros iniciais e alguns feriados. O administrador é criado em Java
com hash BCrypt. Depois de aplicar uma migration, preserve seu conteúdo e
acrescente a próxima versão para mudanças de esquema.

## 5. Fluxo e regras que precisam ser preservados

1. A agenda pública consulta os intervalos dos espaços e recebe a solicitação.
2. O orçamento considera persona, duração, cota, preços, taxas e sinal.
3. Confirmar cria uma reserva `PENDENTE`, com alocação provisória de cota quando aplicável.
4. A equipe usa o CRM para aprovar, recusar, alterar ou cancelar. Pagamentos são registros manuais.
5. O histórico registra ações; as notificações são disparadas depois do commit.

Reservas pendentes e aprovadas bloqueiam o intervalo. Recusadas e canceladas
liberam o horário. Intervalos adjacentes são permitidos; sobreposição é
`novoInicio < existenteFim && novoFim > existenteInicio`.

A cota é compartilhada por persona e mês da reserva, com antecedência mínima
de 15 dias para elegibilidade. A reserva só usa cota se houver saldo para toda
a duração. A aprovação transfere horas pendentes para utilizadas. Cancelamentos
com menos de 30 dias podem gerar débito de duas vezes a duração, conforme as
regras implementadas, com exceção administrativa justificada.

O sinal é 30%, ou 60% em domingos/feriados cadastrados. Valores usam `BigDecimal`
e arredondamento de duas casas. Alterar uma reserva com pagamento registrado
exige regularizar o registro antes do recálculo. Nenhum gateway cobra valores.

Escritas de reserva, cota e cadastros usam a linha de configuração bloqueada
na transação. Isso serializa alterações e ordena a alocação neste MVP; é também
um limite de concorrência a considerar se o volume crescer. Preserve a
atomicidade ao alterar essa estratégia.

Perfis `ADMIN` e `COLABORADOR` têm permissões diferentes. CSRF permanece ativo;
novos formulários de escrita precisam manter o token. A agenda pública não
deve expor contatos ou CNPJ dos solicitantes.

## 6. Validação disponível e seus limites

O ZIP corresponde, arquivo por arquivo, à versão validada anteriormente nesta
entrega. As evidências descritas em [docs/VALIDACAO.md](docs/VALIDACAO.md) incluem:

- 26 testes aprovados: 5 unitários, 19 de integração e 2 de concorrência.
- Compilação, instalação automática de JDK/Maven e execução em Linux x64.
- HTTP real para agenda, API, login administrativo e gravação de um feriado.
- Persistência do registro após parar, executar o configurador novamente e reiniciar.
- Preservação de configuração e banco ao repetir o configurador.
- Checagem de sintaxe Bash, PowerShell, JavaScript e JSON do VS Code.

Esta publicação acrescenta documentação e organização do repositório, sem
alterar o código da aplicação. A suíte não foi executada novamente apenas por
essas mudanças de documentação. Não houve teste nativo Windows/macOS, teste
visual em navegador, envio real por Gmail ou nova execução MySQL nesta versão.
Os prints do Ubuntu não comprovam uma execução completa nesse computador.

Para repetir os testes após a configuração:

```sh
bash maven.sh test
bash maven.sh clean verify
```

No Windows, substitua `bash maven.sh` por `.\maven.bat`.

## 7. Pendências e próximo trabalho

| Prioridade | Pendência | Condição de conclusão |
|---|---|---|
| Antes da apresentação | Confirmar taxas de operação, limpeza e insumos | Valores aprovados cadastrados; zero informado explicitamente quando não houver cobrança |
| Antes da apresentação | Confirmar expediente, horas por turno e capacidade dos espaços | Parâmetros comparados com a operação do Hub |
| Ao planejar novas datas | Cadastrar preços e feriados dos próximos anos | Tabelas do ano e quantidades de turnos necessárias disponíveis |
| Compatibilidade | Executar configuração e demonstração nos PCs da equipe | Registrar SO, comandos e resultado no documento de validação |
| Email | Configurar SMTP e verificar notificações reais | Envio confirmado com credenciais próprias e sem expor senhas |
| Evolução operacional | Definir backup, recuperação e eventual uso compartilhado | Procedimento de restauração validado no banco escolhido |

Limites funcionais atuais: pagamento manual; sem fila de reenvio de email;
validação de CNPJ por formato, sem comprovar existência da empresa; dados
iniciais de demonstração não substituem preços, feriados e cadastros reais.
Não há deploy de produção nem pipeline de CI configurado nesta entrega.

## 8. Leitura recomendada para continuar

Comece pelo [README.md](README.md) e por este handover. Consulte
[docs/REQUISITOS.txt](docs/REQUISITOS.txt) para o material de requisitos recebido,
[docs/VALIDACAO.md](docs/VALIDACAO.md) para o escopo dos testes,
[docs/modelo-conceitual.png](docs/modelo-conceitual.png) para o modelo de referência
e [docs/referencia-agenda.png](docs/referencia-agenda.png) para a referência visual.
Ao implementar a próxima alteração, ajuste o serviço responsável, preserve
as regras acima e rode os testes pertinentes antes de publicar.

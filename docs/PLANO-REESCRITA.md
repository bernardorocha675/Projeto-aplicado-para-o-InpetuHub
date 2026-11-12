# Plano da reescrita do InPETU Hub (base nova `sistema/`)

<!-- Plano aprovado pelo responsável em 11/11/2026. Atualize a seção "Andamento" a cada entrega. -->

## Andamento

| # | Entrega | Branch | Situação |
|---|---|---|---|
| 1 | Fundação | `reescrita/base` | Em revisão (PR aberto, aguardando o ok) |
| 2–12 | — | — | Não iniciadas; cada uma só começa com o ok do responsável |

Versões fixadas na entrega 1 (todas publicadas há mais de 2 semanas em 11/11/2026): Java 25, Maven 3.9.16 (Maven Wrapper 3.3.4, com SHA-256), Jetty 12.1.14 (EE 11), JUnit 5.14.4, Spotless 3.10.4.

## Contexto

A equipe decidiu refazer o InPETU Hub do zero só com Java, SQL, Python e HTML/CSS/JS, tirando Go, Node e Bash (documento `.claude/INICIO-REESCRITA-InPETU-Hub.md`). O sistema antigo (tag `v0.5.12`) continua intacto e em uso até a seção 8 do documento. A base nova nasce em `sistema/`, no mesmo repositório, em branches `reescrita/…`, um PR pequeno por assunto e com os testes escritos antes do código (skill `tdd`). Cada entrega começa só com o ok do responsável e termina parando para aguardar.

## Decisões tomadas (11/11/2026)

| # | Tema | Decisão |
|---|---|---|
| 1 | Como o usuário abre | **Iniciador em Python**, empacotado com PyInstaller, levando um Java mínimo (jlink) e o jar; **atualiza-se pelas Releases do GitHub** com conferência SHA-256 |
| 2 | Banco | **H2 em arquivo (local) + MySQL**, as mesmas migrations Flyway nos dois |
| 3 | Java | **Java puro**: Jakarta Servlet num **Jetty embutido**, **JDBC puro** (sem JPA), **JSP/JSTL** para as telas; CSRF, sessão, limite de login e CSP feitos à mão em filtros |
| 3b | Bibliotecas autorizadas | Jetty (+ JSP/JSTL), Flyway (+ flyway-mysql), H2, MySQL Connector/J, jBCrypt; JUnit 5 nos testes. **Não** entram agora: Angus Mail, libphonenumber, SLF4J/Logback e HikariCP |
| 4 | Dados antigos | **Começar limpo**; nada é importado da pasta `InPETU-Hub/0.4`, que nunca é tocada |
| 5 | Receptor de relatórios | **Abandonado** |
| 6 | Pasta | **`sistema/`** |
| 7 | Python | Iniciador, **build e pacote**, **verificação local** (substitui o `verificar-tudo.sh`) |
| 8 | JDK / formatador | **Java 25 LTS**; **formato Eclipse XML** (`sistema/formatacao-java.xml`, 4 espaços, 120 colunas), lido nativamente pelo VS Code, importado no IntelliJ e conferido com `./mvnw spotless:check` |
| 9 | Versão | Linha **1.0.0**, com pré-releases `1.0.0-alpha.N`; dados em `InPETU-Hub/1`. O iniciador Go antigo recusa versões fora de 0.4/0.5 (`iniciador/model.go:83`), então as instalações antigas não pulam para a nova |
| 10 | Escopo da v1 | **Núcleo + Executável** (abaixo). CRM e Administração ficam para depois, antes de remover o antigo |
| 11 | GitHub | **Só o modelo de PR** (`.github/pull_request_template.md`); sem modelos de issue, rótulos nem quadro por enquanto |
| 12 | Taxas e preços sem Administração | **Limitação aceita.** As regras ficam iguais às do antigo: a aprovação recusa reservas que precisam de taxas ainda não configuradas, e pedidos sem tabela de preços para o ano (2027) recebem uma mensagem amigável. Na prática, a v1 aprova reservas P1 (sem cobrança) e as que não dependem de taxa, em 2026. A tela de Configuração e Preços entra com a Administração |
| 13 | Email | **Confirmação manual na v1**, como o modo local do antigo: a equipe confere a identidade e o contato ao aceitar. O código por email fica atrás da interface `EnviadorEmail`, para quando o Angus Mail for autorizado |

Consequências das bibliotecas recusadas:
- **Telefone:** só a regra de dígitos (Brasil 10–15 com máscara; outros países `+código`, 8–15, E.164), como na regra de 21/10/2026.
- **Logs:** `java.util.logging` com um formatador próprio que só aceita campos permitidos (porta do `SafeLogEncoder`/`FailureCause`).
- **Conexões:** `JdbcConnectionPool` do próprio H2; no MySQL, uma conexão por requisição.
- **Sugestões de cidade:** ficam fora da v1, com campo de texto livre. Ler a resposta do Open-Meteo sem biblioteca de JSON seria frágil.

## Escopo da v1

**Núcleo**
- login e perfis `ADMIN`/`COLABORADOR`, com as 5 contas de demonstração restauradas no modo local e troca de senha;
- espaços: os 10 do Hub e um cadastro próprio;
- agenda pública e `/disponibilidade`;
- pedido de reserva → resumo → confirmação → sucesso;
- solicitações: aceitar, com conferência de identidade, ou recusar;
- reservas: lista, busca, detalhe e histórico; aprovar, recusar, cancelar (com força maior), concluir e registrar pagamento;
- regras de preço, sinal, cota e expiração;
- auditoria gravada no banco (a tela fica para a Administração).

**Executável**
- iniciador Python;
- pacote por sistema (Windows, macOS e Linux, x64/ARM64);
- atualização pelas Releases;
- CI em matriz.

**Depois da v1:** tela de Configuração e Tabela de preços (que destrava a aprovação das reservas cobradas e as de 2027), CRM (empreendedores com perfil, atendimentos, visitas, colaboradores), Administração (painel, tela de auditoria, feriados, personas, tabela de preços, configuração, cotas), código por email, sugestões de cidade e telefone por país.

## Estrutura

```
sistema/                    projeto Maven (mvnw, Java 25, Jetty embutido, jar único)
  src/main/java/br/com/inpetu/hub/
    comum/        servidor, DataSource, transação JDBC, Clock, filtros (cabeçalhos/CSP, CSRF, host local,
                  sessão, limite), erros, log seguro, JSON mínimo
    conta/  espaco/  agenda/  reserva/  solicitacao/  cota/  auditoria/  empreendedor/ (só o vínculo na v1)
  src/main/resources/db/migration/   V1__… (Flyway)
  src/main/webapp/WEB-INF/jsp/<assunto>/, static/css (tokens, componentes, publica, interna), js, img, fontes
  src/test/java/…           espelha os pacotes
inicializador/              iniciador Python (nome provisório, para não colidir com o iniciador/ em Go)
scripts/                    Python: build_pacote.py, verificar.py, tests/
docs/PLANO-REESCRITA.md, CONTRIBUTING.md, .editorconfig, .gitattributes, .vscode/, .run/
.github/workflows/sistema.yml (+ sistema-release.yml), pull_request_template.md
```

Regras: pacotes por assunto; entidades encapsuladas com métodos de regra (`reserva.aprovar(...)`); um formulário e um serviço por cadastro (sem reflexão); ordem fixa nos selects; Sim/Não em vez de true/false; classes com menos de ~300 linhas.

## O que se reaproveita do antigo (consulta, sem copiar estrutura)

- **Regras:**
  - `aplicativo/.../service/ReservaService.java`, `PrecoService.java`, `TaxacaoService.java`, `CotaService.java`, `DisponibilidadeService.java`, `PublicRequestService.java`, `ExpirationService.java`;
  - `validation/Documentos.java`, `ContactRules.java`, `ReservationDates.java`.
- **Segurança e logs:** `security/SecurityConfig.java` (CSP e cabeçalhos exatos), `AbuseFilter.java`, `SessionGuardFilter.java`, `LocalHostFilter.java`, `observability/SafeLogEncoder.java`, `FailureCause.java`.
- **Modelo de dados:** migrations `V1…V6`.
- **Visual:**
  - `static/css/site.css` (tokens);
  - `static/js/agenda.js`, `menu.js`, `password-toggle.js`;
  - fontes e imagens locais;
  - `templates/fragments.html` (estrutura acessível).
- **Testes de apoio:** `test/.../Css.java` e `Html.java` (contraste e aria calculados nos testes).
- **163 métodos de teste** como especificação: cada entrega lista os testes antigos que reescreve.
- **Pacote e verificações:** `.github/scripts/package_v04.py` (jlink, `bundle.json`) e `setup_maven.py` (novas tentativas), como referência para o Python novo.

## Entregas (cada uma = 1 branch + 1 PR + parar)

1. **Fundação** (`reescrita/base`):
   - `sistema/` com `pom.xml`, Maven Wrapper, Spotless com Eclipse XML e Jetty subindo `/saude`;
   - `.editorconfig`, `.gitattributes`, `.vscode/` (extensions, launch, tasks, settings) e `.run/` (Rodar sistema, Todos os testes);
   - `CONTRIBUTING.md`, `sistema/README.md`, `docs/PLANO-REESCRITA.md` (este plano) e `CLAUDE.md` atualizado (a reescrita, a pasta nova e o antigo intacto);
   - modelo de PR (só ele);
   - workflow `sistema.yml` (compilar, testar, `spotless:check`, gitleaks, com novas tentativas e tempo-limite);
   - `protecao.py` passa a proteger também `sistema/src/main/resources/db/migration/`, com um teste novo em `test_protecao.py`.
   - **Teste que falha primeiro:** `ServidorTest`: sobe numa porta livre e `/saude` responde 200.
2. **Banco:**
   - `V1__modelo.sql` consolidado a partir de V1–V6, com nomes corrigidos (`documento` + `tipo_documento`, sem colunas de MFA nem tabelas de importação);
   - dados iniciais: personas, configuração, preços de 2026, feriados e os 10 espaços.
   - **Testes:** a migration roda em H2 e em MySQL (serviço MySQL na CI), e os dados iniciais conferem.
3. **`comum/`, segurança web e erros:**
   - CSP e cabeçalhos idênticos aos do antigo;
   - CSRF por token em sessão, `LocalHostFilter` e páginas de erro sem detalhe técnico;
   - log seguro em JUL;
   - layout JSP com "Pular para o conteúdo";
   - CSS de tokens.
   - **Testes portados:** `headersCsrfAndEscapingArePreserved`, `HandledErrorsIntegrationTest`, `LocalHostFilterTest`, parte do `LoggingTest`, `umSoRootComOsTokens`, contraste.
4. **`conta/`:**
   - login por formulário, BCrypt, sessão de 20 min com prazo absoluto e cookies `HttpOnly`/`SameSite`;
   - limite de tentativas e auditoria do login;
   - perfis, as 5 contas de demonstração restauradas no modo local e troca de senha.
   - **Testes portados:** `PublicDemoLoginTest`, `collaboratorCannotReadOrWriteAdministration`, `rateLimitDoesNotPermanentlyLockVictim`, `passwordChange…`.
5. **`espaco/` + `agenda/`:**
   - cadastro de espaços (formulário próprio);
   - agenda pública, `/disponibilidade` em JSON mínimo e `agenda.js`.
   - **Testes portados:** `EspacosCadastroIntegrationTest`, `AgendaLegivelTest`, `espacoInativoNaoSignificaAgendaOcupada`, `consultaAutomaticaDaAgendaSemCookieNaoCriaSessao`.
6. **Regras de reserva (domínio puro):**
   - `Reserva` encapsulada, disponibilidade, preço (turnos, persona e sinal de 30%/60%), taxação, cota (só reservas aprovadas consomem; penalidade 2× abaixo de 30 dias; elegível com 15 dias de antecedência) e janela até 31/12 do ano seguinte.
   - **Testes portados:** `RegrasTest`, `ReservationDatesTest` e os testes de regra do `FluxosIntegrationTest` (`cancelamentoTardioDuplicaHorasEPropagaDivida`, `menosDe15DiasNaoUtilizaCota` etc.), com `Clock` fixo.
7. **`solicitacao/`, o pedido público:**
   - formulário (documento por tipo, telefone com regra de dígitos, empresa opcional), resumo, confirmação manual e sucesso;
   - idempotência por chave e expiração em 24 h (`ScheduledExecutorService`);
   - erros ligados aos campos.
   - **Testes portados:** `PedidoReservaOficialTest`, `DocumentosTest`, `publicIntakeDoesNotPoisonCrmOrReserveQuotaAndIsIdempotent`, os itens de `RelatorioBugsIntegrationTest`.
8. **Área Interna de reservas:**
   - solicitações (aceitar com conferência ou recusar);
   - reservas: lista, busca, detalhe com Sim/Não e histórico, e todas as ações;
   - auditoria gravada;
   - menu com hambúrguer.
   - **Testes portados:** o restante do `FluxosIntegrationTest`, `ConcorrenciaIntegrationTest`, `MenuCelularTest`, `AreaInternaTest`.
   - **Com isso a fatia de ponta a ponta fica completa:** login → espaços → pedido → aprovação.
9. **Iniciador Python, parte 1:**
   - janela tkinter com Iniciar, Parar e Abrir navegador;
   - instância única (lock) e porta livre em 127.0.0.1;
   - `runtime/bin/java -jar app.jar`;
   - **encerramento ordenado por arquivo de parada**, com força só depois do prazo;
   - backup do banco antes de iniciar e log local sem dados pessoais;
   - pasta `InPETU-Hub/1`.
   - **Testes:** `unittest` com um processo Java falso.
10. **Iniciador Python, parte 2 (atualização):**
    - API pública de Releases, sem token, aceitando só tags `1.x`;
    - `SHA256SUMS`, extração segura e preservação da versão anterior;
    - falhas tipadas: limite, tempo esgotado e rede.
    - **Testes:** `http.server` local simulando 403, 429, 500, tempo esgotado e release incompleta, como os testes Go antigos.
11. **Pacote e CI de release:**
    - `scripts/build_pacote.py`: jar, jlink, PyInstaller, `bundle.json` com SHA-256 e ZIP;
    - `scripts/verificar.py`: tudo localmente;
    - `sistema-release.yml`: matriz de 6 alvos, smoke do pacote e pré-release `1.0.0-alpha.N` só com o ok do responsável.
    - **Risco a validar cedo:** PyInstaller no Windows ARM64 e o `.app` do macOS.
12. **v1.0.0:** revisão (`code-review`, `revisor-privacidade-logs`, `web-design-guidelines`, `accessibility-a11y`) e release com notas simples, com o ok do responsável.

Em toda entrega:
- `tdd` (o teste falha antes) e `verification-before-completion` (mostrar a saída);
- informar o sistema e o editor em que foi testado e onde não foi;
- atualizar `docs/PLANO-REESCRITA.md` e `continuidade/ESTADO-ATUAL.md`.

## Antes de começar a entrega 1

- Reabrir o Claude Code na raiz `InPETU-Hub/`, para carregar as skills, os hooks e o subagente do repositório.
- Criar a branch `reescrita/base` a partir de `origin/main`.

## Verificação

- `cd sistema && ./mvnw verify` (testes, `spotless:check`) no Linux local; a CI repete em ubuntu e com MySQL.
- A partir da entrega 3: subir com `./mvnw exec:java -Dperfil=local` (ou a configuração "Rodar sistema" do IntelliJ ou do VS Code) e conferir as telas no navegador.
- Na entrega 1: abrir o projeto do zero no IntelliJ e no VS Code e registrar no PR o que foi e o que não foi testado.
- Entregas 9–11: `python -m unittest` em `inicializador/` e `scripts/`, e smoke do pacote na matriz da CI.

# Plano da reescrita

O InPETU Hub foi refeito do zero com Java, SQL, Python e HTML/CSS/JS, sem Go, Node e Bash. O plano foi aprovado pelo responsável em 11/11/2026. A versão completa dele, com o detalhe e os testes de cada entrega, está no [histórico](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/blob/7129fccce3aedc9590a69b2ab4a5655a5a89910f/docs/PLANO-REESCRITA.md).

## Andamento

| Etapa | Situação |
| --- | --- |
| 1 a 11: fundação, banco, segurança, conta, espaços e agenda, regras, pedido, Área Interna, iniciador, atualização e pacote | Na `main` (PRs #58 a #68) |
| Windows | Na `main` (PR #69); só x64 desde o PR #72 |
| 12: revisão | Na `main` (PR #71) |
| 1.0.0-alpha.1 | Publicada em 23/11/2026 |
| 1.0.0-alpha.2: registro dos testes e relatório | Publicada em 23/11/2026 |
| **Teste do time e correções (alpha.3)** | **Próxima** |
| v1.0.0 | Depois, só com o ok do responsável |

## Decisões

| # | Tema | Decisão |
| --- | --- | --- |
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
| 11 | Sistemas | **Só Windows x64 e Linux x64** (23/11/2026). Antes: Linux primeiro (12/11) e Windows x64 e ARM64 (17/11). macOS e ARM64 fora. O sistema antigo mantém a matriz dele |
| 12 | Taxas e preços sem Administração | **Limitação aceita.** As regras ficam iguais às do antigo: a aprovação recusa reservas que precisam de taxas ainda não configuradas, e pedidos sem tabela de preços para o ano (2027) recebem uma mensagem amigável. Na prática, a v1 aprova reservas P1 (sem cobrança) e as que não dependem de taxa, em 2026. A tela de Configuração e Preços entra com a Administração |
| 13 | Email | **Confirmação manual na v1**, como o modo local do antigo: a equipe confere a identidade e o contato ao aceitar. O código por email fica atrás da interface `EnviadorEmail`, para quando o Angus Mail for autorizado |
| 14 | Telas só para computador (16/11/2026) | A reescrita é feita **só para computador**: nada de layout, testes ou conferências para celular. O CSS de celular que já existe fica como está, mas não recebe trabalho novo. A página inicial continua **básica** (`/` abre a agenda). O cabeçalho e o rodapé oficiais entram na **entrega 8**, só para computador. Os **feriados de 2027** ficam como estão, sem ação, até novo pedido |
| 15 | GitHub | **Só o modelo de PR** (`.github/pull_request_template.md`); sem modelos de issue, rótulos nem quadro por enquanto |

Consequências das bibliotecas recusadas:
- **Telefone:** só a regra de dígitos (Brasil 10–15 com máscara; outros países `+código`, 8–15, E.164), como na regra de 21/10/2026.
- **Logs:** `java.util.logging` com um formatador próprio que só aceita campos permitidos (porta do `SafeLogEncoder`/`FailureCause`).
- **Conexões:** `JdbcConnectionPool` do próprio H2; no MySQL, uma conexão por requisição.
- **Sugestões de cidade:** ficam fora da v1, com campo de texto livre. Ler a resposta do Open-Meteo sem biblioteca de JSON seria frágil.

## Regras de negócio decididas

- **Reservas:** só a aprovada ocupa horário e usa cota. Pedidos e reservas pendentes vencem em 24 horas. A janela vai de hoje a 31/12 do ano seguinte.
- **Cota:** exige 15 dias de antecedência e não acumula. Cancelar com menos de 30 dias desconta 2× as horas; a dívida passa para o mês seguinte.
- **Evento já terminado:** não é cancelado, é concluído. A exceção é força maior do gestor, com justificativa.
- **Sinal:** 30%, ou 60% em domingo e feriado.
- **Pagamento e identidade:** conferidos à mão pela equipe. O sistema não cobra.
- **Espaços:** capacidade é o número de cadeiras; o resto vai em mobiliário. O que a equipe editar ou desativar prevalece.
- **Documento:** CNPJ (14 dígitos), CPF (11), estrangeiro (4 a 30) ou Sem documento. Só a quantidade é conferida. Sem documento, o cadastro é achado pelo email. Empresa é opcional.
- **Contato:** nome sem números, email com domínio completo e telefone com DDD, ou com + e o código do país.
- **Visual:** o do site oficial do Hub. Links externos só depois de conferidos e em nova aba. Agenda e Área Interna ficam sempre visíveis.
- **Uso:** o usuário final só abre o executável. Os dados ficam no computador de cada pessoa e não são compartilhados entre máquinas.

As decisões da 0.5 e o porquê de cada uma estão no [contexto antigo](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/blob/cecd3df6f737ab04bf8055da9c33f4e898ae858e/continuidade/CONTEXTO.md), e a operação dela no [mapa antigo](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/blob/cecd3df6f737ab04bf8055da9c33f4e898ae858e/continuidade/OPERACAO.md).

## O que a 1.0 tem

- Login, perfis `ADMIN` e `COLABORADOR`, cinco contas de demonstração e troca de senha.
- Cadastro de espaços, com os 10 do Hub; agenda pública e `/disponibilidade`.
- Pedido de reserva, com resumo e conferência manual.
- Solicitações: aceitar ou recusar.
- Reservas: lista, busca, detalhe, histórico, aprovar, recusar, alterar, cancelar, concluir e registrar pagamento.
- Regras de preço, sinal, cota e vencimento; auditoria no banco.
- Iniciador, atualização pelas Releases e pacotes de Windows e Linux.

## O que falta

**Depois da v1:**
- Configuração e tabela de preços, que liberam as reservas cobradas e as de 2027.
- Empreendedores com perfil, atendimentos, visitas e colaboradores.
- Administração: tela de auditoria, feriados, personas e cotas.
- Código por email; sugestões de cidade e telefone por país.

**Para o responsável decidir:**
- Fazer Configuração e preços **antes** da v1.0.0? Sem elas, só a persona P1 é aprovada.
- Rodar com MySQL (hoje só nos testes de migração) e a interface `EnviadorEmail` (não existe) ficam para depois da v1?
- Quando aposentar o código da 0.5. A decisão 10 pede CRM e Administração prontos antes.
- Lotes 5 e 6 do [plano da interface](PLANO-INTERFACE.md) da 0.5: abandonar?

**Dívidas conhecidas:** `SolicitacaoServico` com mais de 300 linhas; estados do pedido como texto; limite 429 repetido em três lugares.

## Riscos ainda não testados

- O pacote num Windows fora da CI, e o aviso de programa não assinado.
- A atualização entre versões publicadas no Windows (no Linux, conferida da alpha.1 para a alpha.2).
- Telas com MySQL, leitor de tela real e Linux além do Ubuntu.

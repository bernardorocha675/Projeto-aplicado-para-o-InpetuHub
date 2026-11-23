# sistema/ — base nova do InPETU Hub (reescrita 1.x)

Esta é a reescrita do InPETU Hub, em **Java puro**: Jakarta Servlet num Jetty embutido, JDBC e JSP. O plano, as decisões e o andamento estão em [`docs/PLANO-REESCRITA.md`](../docs/PLANO-REESCRITA.md). O sistema antigo (`aplicativo/`, linha 0.5.x) continua intacto e em uso até a reescrita ficar pronta.

## O que você precisa

- **JDK 25** (ex.: Temurin 25). Confira com `java -version`.
- Nada mais: o `./mvnw` baixa o Maven certo (3.9.16) e confere o SHA-256 dele.

## Comandos (dentro de `sistema/`)

| Para | Linux | Windows (o pacote é testado na CI; estes comandos de desenvolvimento não foram rodados no Windows) |
|---|---|---|
| Rodar todos os testes e conferir a formatação | `./mvnw verify` | `.\mvnw.cmd verify` |
| Formatar o código Java | `./mvnw spotless:apply` | `.\mvnw.cmd spotless:apply` |
| Subir o sistema (porta 8080) | `./mvnw compile exec:java` | `.\mvnw.cmd compile exec:java` |
| Gerar o jar único | `./mvnw package` → `target/inpetu-hub-sistema.jar` | igual |
| Rodar o jar | `java -jar target/inpetu-hub-sistema.jar 8080` | igual |

O teste das migrations no MySQL roda só quando existe um MySQL de teste (na CI sempre existe). Para rodar na sua máquina, num banco **vazio e só de teste** (o teste apaga as tabelas dele):

```
INPETU_TESTE_MYSQL_URL="jdbc:mysql://127.0.0.1:3306/inpetu_teste" INPETU_TESTE_MYSQL_USUARIO="inpetu" INPETU_TESTE_MYSQL_SENHA="..." ./mvnw verify
```

Com o sistema de pé, `http://127.0.0.1:8080/saude` responde `ok` `http://127.0.0.1:8080/agenda` abre a agenda pública e `http://127.0.0.1:8080/login`, a Área Interna. Ele escuta só em `127.0.0.1`, nunca na rede.

**Dados e contas (modo local).** O banco H2 fica na pasta de `-Dinpetu.dados="<pasta>"`, que o iniciador vai passar (entrega 9). Sem essa opção, ele fica em `target/dados-desenvolvimento/` (apagado pelo `clean`). Com `-Dinpetu.parada="<arquivo>"` (o iniciador passa), o sistema encerra em ordem quando esse arquivo aparece: fecha o banco e registra `SERVER_STOPPED`. O iniciador fica em [`inicializador/`](../inicializador/README.md). Ao subir, as 5 contas públicas de demonstração (as mesmas do antigo, em [`docs/ACESSOS.md`](../docs/ACESSOS.md)) são criadas ou voltam ao padrão: uma senha trocada vale até o sistema reiniciar.

## No editor

Abra a **raiz do repositório** (não a pasta `sistema/`):

- **VS Code:** aceite as extensões recomendadas (Java Extension Pack e EditorConfig). As configurações **Rodar sistema** (F5) e as tarefas **Todos os testes** e **Formatar Java** já vêm prontas. O formatador lê `sistema/formatacao-java.xml` e formata ao salvar.
- **IntelliJ IDEA:** se o projeto Maven não for detectado sozinho, clique com o botão direito em `sistema/pom.xml` e escolha **Add as Maven Project**. As configurações **Rodar sistema** e **Todos os testes** aparecem no topo (pasta `.run/`). Para formatar igual à CI, importe o formatador: *Settings → Editor → Code Style → Java → ⚙ → Import Scheme → Eclipse XML Profile* → `sistema/formatacao-java.xml`.

A CI (`.github/workflows/sistema.yml`) roda `./mvnw verify`, ou seja, testes e `spotless:check`. Se a formatação falhar, rode `./mvnw spotless:apply`.

## Verificar tudo e gerar o pacote (na raiz do repositório)

- `python3 scripts/verificar.py`: testes e formatação do Java, testes do iniciador (com o jar) e dos scripts; `--pacote` também gera o pacote desta máquina e roda o teste de fumaça.
- `python3 scripts/build_pacote.py --versao 1.0.0-alpha.N`: gera `dist/InPETU-Hub-Linux-x64-v1.0.0-alpha.N.zip` (ou `-Windows-x64-`) e a linha dele no `dist/SHA256SUMS.txt`. Precisa do JDK 25 (jlink/jdeps) e do `python3-tk` e `python3-venv`.
- `.github/workflows/sistema-release.yml`: em PR, gera e testa o pacote no Linux x64 e no Windows x64; a pré-release só sai à mão (Run workflow, a partir da `main`, com “publicar” marcado), depois do ok do responsável.

## Organização

```
src/main/java/br/com/inpetu/hub/
  Aplicacao.java    ponto de entrada (java -jar)
  comum/            servidor, filtros, erros, log seguro, limites de tentativas
  conta/            login, sessão, perfis, contas de demonstração e troca de senha
  auditoria/        auditoria gravada no banco
  espaco/           cadastro de espaços
  agenda/           agenda pública e /disponibilidade (JSON)
  reserva/          regras de reserva (horário, preço, taxas, cota e situações) e a leitura dos dados delas
  solicitacao/      pedido público: formulário, resumo, envio e vencimento em 24 h
  empreendedor/     documento, contato e o vínculo do pedido com o cadastro (o cadastro completo vem depois da v1)
  interna/          Área Interna: painel, solicitações e reservas
src/test/java/…     espelha os pacotes
formatacao-java.xml formatação Java (Eclipse XML: 4 espaços, 120 colunas)
```

Os pacotes são separados por assunto (`conta/`, `espaco/`, `reserva/` …), e não por camada. As regras completas estão no plano.

## Bibliotecas

São só as autorizadas no plano (decisão 3b): Jetty, Flyway, H2, MySQL Connector/J e jBCrypt, mais JUnit 5 nos testes. Até a entrega 4 entraram todas: Jetty (com JSP/JSTL), Flyway, H2, MySQL Connector/J, jBCrypt e JUnit. Para adicionar qualquer outra, é preciso o ok da equipe antes.

O log do sistema usa `java.util.logging`, em JSON, sem dados pessoais. Os avisos do SLF4J ficam silenciados, porque o SLF4J/Logback não foi autorizado.

# sistema/ — base nova do InPETU Hub (reescrita 1.x)

Esta é a reescrita do InPETU Hub, em **Java puro**: Jakarta Servlet num Jetty embutido, JDBC e JSP. O plano, as decisões e o andamento estão em [`docs/PLANO-REESCRITA.md`](../docs/PLANO-REESCRITA.md). O sistema antigo (`aplicativo/`, linha 0.5.x) continua intacto e em uso até a reescrita ficar pronta.

## O que você precisa

- **JDK 25** (ex.: Temurin 25). Confira com `java -version`.
- Nada mais: o `./mvnw` baixa o Maven certo (3.9.16) e confere o SHA-256 dele.

## Comandos (dentro de `sistema/`)

| Para | Linux | Windows (ainda não testado; vem depois) |
|---|---|---|
| Rodar todos os testes e conferir a formatação | `./mvnw verify` | `.\mvnw.cmd verify` |
| Formatar o código Java | `./mvnw spotless:apply` | `.\mvnw.cmd spotless:apply` |
| Subir o sistema (porta 8080) | `./mvnw compile exec:java` | `.\mvnw.cmd compile exec:java` |
| Gerar o jar único | `./mvnw package` → `target/inpetu-hub-sistema.jar` | igual |
| Rodar o jar | `java -jar target/inpetu-hub-sistema.jar 8080` | igual |

Com o sistema de pé, `http://127.0.0.1:8080/saude` responde `ok`. Ele escuta só em `127.0.0.1`, nunca na rede.

## No editor

Abra a **raiz do repositório** (não a pasta `sistema/`):

- **VS Code:** aceite as extensões recomendadas (Java Extension Pack e EditorConfig). As configurações **Rodar sistema** (F5) e as tarefas **Todos os testes** e **Formatar Java** já vêm prontas. O formatador lê `sistema/formatacao-java.xml` e formata ao salvar.
- **IntelliJ IDEA:** se o projeto Maven não for detectado sozinho, clique com o botão direito em `sistema/pom.xml` e escolha **Add as Maven Project**. As configurações **Rodar sistema** e **Todos os testes** aparecem no topo (pasta `.run/`). Para formatar igual à CI, importe o formatador: *Settings → Editor → Code Style → Java → ⚙ → Import Scheme → Eclipse XML Profile* → `sistema/formatacao-java.xml`.

A CI (`.github/workflows/sistema.yml`) roda `./mvnw verify`, ou seja, testes e `spotless:check`. Se a formatação falhar, rode `./mvnw spotless:apply`.

## Organização

```
src/main/java/br/com/inpetu/hub/
  Aplicacao.java    ponto de entrada (java -jar)
  comum/            servidor, filtros, erros, log seguro (cresce a partir da entrega 3)
src/test/java/…     espelha os pacotes
formatacao-java.xml formatação Java (Eclipse XML: 4 espaços, 120 colunas)
```

Os pacotes são separados por assunto (`conta/`, `espaco/`, `reserva/` …), e não por camada. As regras completas estão no plano.

## Bibliotecas

São só as autorizadas no plano (decisão 3b): Jetty, Flyway, H2, MySQL Connector/J e jBCrypt, mais JUnit 5 nos testes. Até agora (entrega 1) entraram apenas o **Jetty** e o **JUnit**. Para adicionar qualquer outra, é preciso o ok da equipe antes.

Ao rodar, o Jetty mostra três avisos `SLF4J(W): No SLF4J providers were found`. Isso é esperado, porque o SLF4J/Logback não foi autorizado. O log do sistema (`java.util.logging`) entra na entrega 3.

# Como contribuir

O repositório tem duas partes:

- **`sistema/` — a reescrita (linha 1.x).** É onde o trabalho novo acontece. Siga [`docs/PLANO-REESCRITA.md`](docs/PLANO-REESCRITA.md).
- **`aplicativo/`, `iniciador/`, `receptor/` — o sistema antigo (linha 0.5.x).** Continua em uso e só recebe correções pedidas.

## Fluxo

1. Uma entrega do plano só começa com o ok do responsável.
2. Crie uma branch a partir da `origin/main`, no padrão `reescrita/<assunto>` (ex.: `reescrita/banco`). Nunca faça commit nem push direto na `main`.
3. **Escreva primeiro o teste que falha.** Rode, veja o teste falhar pelo motivo certo e só então escreva o código.
4. Antes de abrir o PR, rode dentro de `sistema/`:
   ```bash
   ./mvnw verify        # testes + conferência de formatação
   ./mvnw spotless:apply  # se a formatação falhar
   ```
5. Abra **um PR pequeno por assunto** e preencha o modelo: o que muda, qual teste falhou antes, **onde foi testado e onde não foi**.
6. Atualize a seção "Andamento" de `docs/PLANO-REESCRITA.md` e o `continuidade/ESTADO-ATUAL.md`.
7. O merge é feito com o ok do responsável, depois da CI verde.

## Regras do código (sistema/)

- Java 25 puro, com Servlets, JDBC e JSP. **Nenhuma biblioteca nova sem o ok da equipe** (lista autorizada no plano, decisão 3b).
- Pacotes por assunto (`conta/`, `reserva/` …). Cada entidade guarda as próprias regras (`reserva.aprovar(...)`).
- Cada cadastro tem um formulário e um serviço, sem reflexão. Os selects têm ordem fixa. As telas mostram Sim/Não, nunca true/false.
- As classes ficam abaixo de ~300 linhas.
- Formatação: `sistema/formatacao-java.xml` (4 espaços, 120 colunas). O VS Code e o IntelliJ usam o mesmo arquivo (veja `sistema/README.md`).
- **Migrations já publicadas na `main` nunca mudam.** Toda mudança no banco é uma migration nova. O hook do Claude Code bloqueia essas edições.

## Privacidade

- Nada de dados pessoais, senhas, tokens, cookies ou logs brutos no código, nos testes, nos commits ou nos PRs.
- Os logs gravam só campos permitidos (nada de email, documento, telefone ou corpo de formulário).
- `Testes/`, `.secrets/` e `.env*` são áreas protegidas.

## Mensagens de commit

Em português do Brasil, no imperativo e curtas. Exemplo: `Cria a fundação do sistema novo com Jetty e /saude`.

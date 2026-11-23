# Como contribuir

O trabalho novo acontece em `sistema/` (Java), `inicializador/` (Python) e `scripts/` (pacote e testes). As pastas `aplicativo/`, `iniciador/` e `receptor/` são da versão 0.5 e só recebem correções pedidas.

## Preparar

- **JDK 25** e **Python 3.12 ou mais novo**. No Ubuntu, também `python3-tk` e `python3-venv`.
- O Maven vem pelo `./mvnw`. Detalhes em [sistema](sistema/README.md) e [iniciador](inicializador/README.md).

## Fazer uma mudança

1. Combine o que será feito. Uma etapa do [plano](docs/PLANO-REESCRITA.md) só começa com o ok do responsável.
2. Crie uma branch a partir da `origin/main`. Nunca faça commit nem push direto na `main`.
3. **Escreva primeiro o teste e veja ele falhar** pelo motivo certo.
4. Rode a verificação, na raiz:
   ```bash
   python3 scripts/verificar.py            # testes e formatação
   python3 scripts/verificar.py --pacote   # também gera e testa o pacote
   ```
   Se a formatação falhar: `cd sistema && ./mvnw spotless:apply`.
5. Abra **um PR pequeno por assunto**. Diga o que muda, qual teste falhou antes, **onde testou e onde não testou**.
6. Atualize o [estado atual](continuidade/ESTADO-ATUAL.md) e, se for o caso, o plano.
7. O merge é feito com a CI verde e o ok do responsável.

Commits em português do Brasil, curtos e no imperativo, depois dos testes: `Cria a tela de preços`.

## Regras do código

- **Java 25 puro**, com Servlets, JDBC e JSP. No Python, só a biblioteca padrão. Biblioteca nova só com o ok da equipe (decisão 3b do plano).
- Pacotes por assunto. Cada entidade guarda as próprias regras (`reserva.aprovar(...)`).
- Um formulário e um serviço por cadastro, sem reflexão. Selects com ordem fixa. Nas telas, Sim/Não, nunca true/false.
- Classes com até cerca de 300 linhas.
- **Migration publicada na `main` nunca muda**; crie uma nova.
- Pastas podem ter espaços e acentos: use aspas.
- Telas só para computador, com contraste WCAG AA e uso por teclado.

## Privacidade

- Nada de dados pessoais, senhas, tokens, cookies, banco ou logs brutos em código, testes, commits ou PRs.
- O log só grava campos permitidos. Mensagens de exceção nunca entram: podem ter caminhos e dados digitados.
- Nunca coloque credencial de escrita do GitHub no executável.
- `Testes/`, `.secrets/` e `.env*` são áreas protegidas.

## Publicar uma versão

Só com o ok do responsável, a partir da `main` e com a CI verde:

1. Escolha um número novo da linha 1.x. Nunca mova uma tag nem troque um arquivo publicado.
2. Em *Actions → Sistema (reescrita) — pacote e pré-release → Run workflow*, informe a versão e marque **publicar**. Ou: `gh workflow run sistema-release.yml --ref main -f versao=1.0.0-alpha.N -f publicar=true`.
3. Confira: dois pacotes e o `SHA256SUMS.txt`, somas dos arquivos baixados, fumaça do pacote e releases antigas iguais.
4. Atualize os links do README e o estado atual.

O fluxo publica sempre como **pré-release**; para a 1.0.0 final, ele precisa ser ajustado antes.

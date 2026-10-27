# Estrutura e desenvolvimento

## Fontes ativos

O repositório mantém uma única árvore de fontes atuais. `aplicativo/` contém Java/Spring Boot, Maven e os testes; `iniciador/` contém Go e a interface embutida; `receptor/` contém o serviço Node auxiliar dos relatórios. O Java é uma dependência real do executável e acompanha os pacotes como runtime privado.

`.github/scripts/` e `.github/workflows/` constroem, testam e publicam os pacotes. `aplicativo/scripts/Testar.java` é a ferramenta interna de verificação (`Backup.java`, ferramenta antiga de manutenção sem teste automático, aguarda decisão de remoção), não uma segunda forma de iniciar o produto. `docs/`, `continuidade/` e `Testes/` reúnem documentação e registros necessários à entrega.

As pastas versão 0.1, versão 0.2, versão 0.3 e versão 0.4 saíram da árvore atual. Seus conteúdos continuam no histórico Git, nas tags/releases e no backup anterior à reorganização. Não houve reescrita do histórico nem alteração de assets antigos. Para consultar uma entrega antiga, use sua tag numa cópia separada.

## Compilar e testar

Estes comandos são para desenvolvedores. O usuário final abre o pacote da release sem instalar ferramentas externas. Para desenvolvimento, prepare JDK 21, Python 3, Go compatível com o pin da CI e Node 22 ou posterior. A CI mantém Go 1.27.2; confira o workflow antes de mudar versões.

Na raiz:

```sh
node .github/scripts/test_agenda_ui.cjs
node --test receptor/test.mjs
```

Na pasta `iniciador`:

```sh
go test ./...
go vet ./...
```

Com Maven no PATH, na raiz:

```sh
mvn -B -ntp -f aplicativo/pom.xml test
```

O roteiro completo, executado **dentro de aplicativo**, usa JDK e Maven no PATH:

```sh
java "-Dfile.encoding=UTF-8" --source 17 scripts/Testar.java
```

A CI usa `python .github/scripts/setup_maven.py` para baixar Maven 3.9.9 em `aplicativo/.tools`, conferindo o SHA-512 oficial. Fora de Actions, o script imprime o caminho; acrescente o diretório bin correspondente ao PATH do terminal de desenvolvimento. Não há instalação global nem preparação do computador do usuário final.

Depois do roteiro, o empacotamento usa JAVA_HOME e INPETU_GOOS/INPETU_GOARCH coerentes com a máquina. A referência completa é `.github/workflows/release-v0.4.yml`. `package_v04.py` gera o pacote, e `smoke_v04.py` testa o executável real com dados descartáveis.

## Formatar templates e CSS

Templates (`aplicativo/src/main/resources/templates`) e CSS (`static/css`) ficam formatados com o Prettier 3.6.2, sem configuração no repositório. O teste `TemplatesFormatadosTest` recusa linhas compactadas. O Prettier é ferramenta de desenvolvimento e não entra no pacote. Na pasta `aplicativo/src/main/resources`:

```sh
npx prettier@3.6.2 --no-config --print-width 140 --html-whitespace-sensitivity css --end-of-line lf --write "templates/**/*.html" "static/css/*.css"
```

`--html-whitespace-sensitivity css` mantém os espaços que aparecem na tela; por isso algumas tags terminam com `>` no começo da linha seguinte. O parser do Prettier não aceita `<th:block>`: troque por `<th-block>` antes de formatar e volte depois (hoje só em `admin/crud/form.html`). Em `admin/crud/lista.html`, as ações da linha ficam com `<!-- prettier-ignore -->`: o `<form>` de Excluir é `inline-block` no CSS e o Prettier, que o trata como bloco, poria entre ele e Editar um espaço que aparece na tela. Testes que comparam trechos do HTML usam `Html.compacto`/`Html.contem`, que ignoram recuo e quebras de linha.

## O que permanece estável

Banco e configurações ficam no perfil do usuário, em `InPETU-Hub/0.4`. Os nomes dos assets e o protocolo bundle.json permanecem compatíveis com a atualização da família 0.4.x. Reorganizar fontes não altera os diretórios das instalações existentes.

Migrações, regras, contas, senhas, permissões e conteúdo de negócio foram preservados nesta reorganização. Mantenha a cópia do banco, chave e ambiente ao recuperar uma instalação. Não usar dados reais nos testes nem versionar arquivos gerados em .tools, target, dist, data, .secrets, backups ou logs.

Novas entregas seguem [RELEASES.md](../RELEASES.md). O ponto de retomada está em [continuidade](../continuidade/README.md); a ativação do receptor permanece uma etapa separada, pendente de confirmação.

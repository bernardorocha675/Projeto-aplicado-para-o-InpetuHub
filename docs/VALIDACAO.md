# 0.5.4 — em revisão

Pedido de 21/10/2026 (lote 4 do Caça bugs 2, reformulado pelo responsável): uma API que sugira cidades do mundo todo e telefone internacional, sem a trava só para o Brasil. Mudanças: `CidadeService` consulta a geocodificação pública do Open-Meteo (sem chave) pelo servidor, com termo validado (2 a 60 letras), tempo limite, cache de 200 termos, só localidades habitadas (`feature_code` PPL…) e texto “Cidade, Estado, País”; sem resposta, lista vazia e evento `CITY_SEARCH_UNAVAILABLE` só com o tipo da falha. Rota pública `/cidades` (`CidadeController`, `SecurityConfig`, `SafeRoutes`). `admin.js`: sugestões (datalist) nos campos Cidade do pedido de reserva e do cadastro de empreendedores; telefone que começa com + fica sem a máscara do Brasil. `ContactRules.PHONE`: Brasil 10–15 dígitos ou + com 8–15 dígitos; mensagens e o campo do formulário público atualizados. Política de segurança: uma linha sobre o envio só do termo digitado. Testes locais (Linux x64, 21/10/2026): 131 Java (novos: sugestões com estado/país, sem duplicados e sem aeroporto via API falsa local, cache e termos inválidos que não saem; API indisponível sem sugestões; telefones do Brasil e internacionais aceitos e recusados; `/cidades` pública), Node de cadastros (6: telefone internacional e sugestões de cidade com falha de rede) e da agenda (11), Testar 14/14, pacote Linux x64 e smoke (rota `/cidades` pública) aprovados; padrão do telefone conferido também como regex do navegador. API real: `CidadeService` compilado consultou o Open-Meteo pelo proxy deste ambiente (Florianópolis, São José, Lisboa, Tóquio com estado e país); o pacote rodando sem proxy configurado devolveu lista vazia e registrou a falha, como previsto. Windows, macOS e ARM64 ficam com a matriz.

---

# 0.5.3 — publicada e verificada

Pedido de 20/10/2026 (documento “Nome dos Espaços”, lote 3 do Caça bugs 2): os 10 espaços reais do Hub pré-cadastrados e mantidos nas atualizações. Decisões do responsável: capacidade = cadeiras e campo novo de mobiliário; publicar como 0.5.3. Mudanças: migration aditiva `V5__espacos_do_hub.sql` (coluna `mobiliario` e os 10 espaços, só os nomes que faltam, sem alterar existentes); `Espaco.mobiliario`; campo opcional “Mobiliário” no cadastro; agenda e pedido de reserva mostram o mobiliário no lugar de “até N pessoas” quando informado (`agenda.html`, `reserva-form.html`, `agenda.js`, JSON de `/disponibilidade`). Testes locais (Linux x64, 20/10/2026): 127 Java (novo `EspacosDoHubMigrationTest`: banco na V4 com “auditório” e um espaço da equipe migra para 11 espaços, sem duplicar nem alterar o existente; `PublicDemoLoginTest`: banco novo com 10 espaços livres e mobiliário na agenda; `FluxosIntegrationTest` passa a desativar os espaços da migration para medir a agenda com uma sala), Node da agenda (11, um novo do mobiliário) e de cadastros (4), Testar 14/14, pacote Linux x64 e smoke (banco novo com 10 espaços livres e mobiliário na agenda) aprovados. Banco real em uso: instalação 0.5.2 com banco existente atualizada pelo pacote 0.5.3 (backup criado), agenda com os 10 espaços e mobiliário. Windows, macOS e ARM64 ficam com a matriz.

A [0.5.3](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.5.3) foi publicada e verificada em 20/10/2026 pelo [run 39490661535](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39490661535), na primeira tentativa, após o PR [#24](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/pull/24) (matriz verde de primeira). Tag/código testado `826e21b883804199d15d59eda67a0bf27ae871b2` (merge em main). Conferidos os 12 assets e os 11 checksums contra digests públicos; v0.5.3 é a latest; as 26 releases anteriores mantêm arquivos, tamanhos e digests; os 8 links do README respondem. Relatório da CI: `Testes/teste29-2026-10-20_17-17-12-700efbfa/log.txt`. Depois da publicação, uma instalação Linux x64 da 0.5.2 com banco existente atualizou sozinha pelo GitHub para a 0.5.3 e a agenda passou a mostrar os 10 espaços.

---

# 0.5.2 — publicada e verificada

Pedido de 20/10/2026, depois do relato do time: com a 0.4.17 aberta, abrir o executável da 0.5.1 continuava mostrando a 0.4.17 (“Usando a versão 0.4.17; a atualização 0.5.1 será tentada na próxima abertura”). Causa: fechar a aba do navegador não fecha o iniciador; depois de uma falha de atualização, o mesmo processo não tentava de novo nem pelo botão, e o executável novo só reabria a tela do antigo. O responsável confirmou que fechar o iniciador e abrir de novo resolve no PC dele e autorizou a 0.5.2. Mudanças no iniciador: Configurar/atualizar limpa as atualizações adiadas e tenta de novo (o Iniciar automático continua sem repetir); um executável cujo pacote é mais novo que o instalado instala a partir dele mesmo, com o backup de sempre, sem novo download (`useNewerEmbedded`); um executável mais novo fecha o iniciador antigo pelo mesmo pedido do botão Fechar iniciador quando o sistema está parado e sem operação em andamento, e assume o lock (`replaceOlderLauncher`/`waitForLock`). Testes locais (Linux x64, 20/10/2026): 54 Go (+race, vet nos seis alvos, gofmt), três novos que falham sem a correção (repetir pelo botão, pacote do executável aberto sem download e com backup, antigo parado fechado e antigo em uso/ocupado/mesma versão preservado), Testar 14/14 (126 Java), receptor 7, pacote Linux x64 e smoke aprovados. Cenário real: pacote 0.4.17 publicado aberto e parado; ao abrir o pacote 0.5.2, a 0.4.17 encerrou a sessão, a 0.5.2 instalou o próprio pacote (backup criado, chave preservada) e iniciou o sistema. Windows, macOS e ARM64 ficam com a matriz.

A [0.5.2](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.5.2) foi publicada e verificada em 20/10/2026 pelo [run 39465961285](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39465961285), na primeira tentativa, após o PR [#23](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/pull/23) (matriz verde de primeira). Tag/código testado `5ee8df5edbe19384e7c682c2fe16471467b58dae` (merge em main). Conferidos os 12 assets e os 11 checksums contra digests públicos; v0.5.2 é a latest; as 25 releases anteriores mantêm arquivos, tamanhos e digests; os 8 links do README respondem. Relatório da CI: `Testes/teste28-2026-10-20_14-25-39-7fcce8e7/log.txt`. Depois da publicação, o pacote Linux x64 da 0.4.17 publicado, numa pasta nova, baixou a 0.5.2 do GitHub, passou para o iniciador da 0.5.2 e iniciou o sistema.

---

# 0.5.1 — publicada e verificada

Testes locais (Linux x64, 19/10/2026): 126 Java (rota pública: Início com conteúdo oficial, Agenda em /agenda, links oficiais em nova aba, sem /sobre nem termos-de-uso, fontes/imagens/CSS servidos; navegação local mantida em 15 telas públicas e internas), Go, Node da agenda (10) e de cadastros (4), Testar 14/14, pacote Linux x64, smoke (regra nova: externos só do domínio oficial e em nova aba) e segredos aprovados. Capturas da instância local comparadas às do site oficial em 1440 px e 640 px. Windows, macOS e ARM64 ficam com a matriz.

A [0.5.1](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.5.1) foi publicada e verificada em 19/10/2026 pelo [run 39344988485](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39344988485), após o PR [#22](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/pull/22). Tag/código testado `3dc211d1e81fa198df6471de45ff2a0a97ef3e99` (merge em main). Conferidos os 12 assets e os 11 checksums contra digests públicos; v0.5.1 é a latest; as 24 releases anteriores mantêm arquivos, tamanhos e digests; os 8 links do README (página da release, código completo e 6 pacotes) respondem. Relatório da CI: `Testes/teste27-2026-10-19_21-00-12-9532bf2a/log.txt`. Caminho até a publicação: PR [#21](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/pull/21) (visual) com matriz verde; a primeira execução da main ([run 39342720435](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39342720435)) falhou no windows-2025 em “Banco e senha descartável persistem após reiniciar”. Causa: o `Testar.java` encerrava o servidor com `Process.destroy`, que no Windows é forçado, e o H2 perdia a senha recém-trocada. O aplicativo não tem esse problema, pois o iniciador encerra pelo pedido `.secrets/launcher.stop`. O PR #22 fez o roteiro usar o mesmo pedido (força só após 20 s); Testar 14/14 local e matriz verde nos seis sistemas.

---

# 0.5.0 — publicada e verificada

A [0.5.0](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.5.0) foi publicada e verificada em 19/10/2026 pelo [run 39337208760](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39337208760), na primeira tentativa, após o PR [#20](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/pull/20). Tag/código testado `29602bae776c8981a925d5771d5953f63a447229`. Conferidos os 12 assets e os 11 checksums contra digests públicos; v0.5.0 é a latest; as 23 releases anteriores mantêm arquivos, tamanhos e digests; os 8 links do README novo (página da release, código completo e 6 pacotes) respondem. Relatório da CI: `Testes/teste25-2026-10-19_19-45-58-54005acb/log.txt`. A 0.4.17 também foi publicada e verificada (run 39335067420).

Mesmo código da 0.4.17, só com a versão 0.5.0 (VERSION, POM, iniciador, receptor, roteiro), notas, README e o texto do LEIA-PRIMEIRO do pacote. Testes locais (Linux x64, 19/10/2026): Go test/vet, 126 Java, Testar 14/14, pacote Linux x64, smoke (inclui a troca real do iniciador 0.5.0 → 0.5.1 na mesma pasta) e segredos aprovados. Windows, macOS e ARM64 ficam com a matriz. Teste real da passagem de linha: pacote Linux x64 da 0.4.17 (commit edd985a) com o smoke forçando a troca para um iniciador 0.5.0 — troca real do iniciador, 15 logins, chave e banco preservados e recuperação aprovados.

---

# Fix 0.4.17 — em revisão

Testes locais (Linux x64, 19/10/2026): Go test/vet/race e gofmt (dois testes novos: a 0.4 escolhe a 0.5.0 e não outra linha; a 0.4.17 entrega a execução ao iniciador da 0.5.0 instalado na mesma pasta), receptor, 126 Java (filtro de versão dos logs aceita 0.5), Testar 14/14, pacote Linux x64, smoke e segredos aprovados. Windows, macOS e ARM64 ficam com a matriz.

---

# Fix 0.4.16 — publicada e verificada

A [0.4.16](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.16) foi publicada e verificada em 19/10/2026 pelo [run 39330445496](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39330445496), na primeira tentativa, após o PR [#18](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/pull/18) (matriz verde de primeira). Tag/código testado `ae99fe07aca99b95093d2f7e139a8eee4572e55d` (merge em main). Conferidos os 12 assets e os 11 checksums contra digests públicos; v0.4.16 é a latest; as 21 releases anteriores mantêm arquivos, tamanhos e digests; links do README respondem. Relatório da CI: `Testes/teste23-2026-10-19_18-49-14-2e3f29e6/log.txt`.

Testes locais (Linux x64, 19/10/2026): 126 Java (página pública confere o texto “Clique em um dia marcado”), Node da agenda (10, reescritos para o novo visual: dia azul com ponto, dia branco sem texto, título do mês, aviso só para a equipe) e de cadastros (4), Testar 14/14, pacote Linux x64, smoke e segredos aprovados; captura de tela com dados simulados comparada à referência do PDF. Windows, macOS e ARM64 ficam com a matriz.

---

# Fix 0.4.15 — publicada e verificada

A [0.4.15](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.15) foi publicada e verificada em 19/10/2026 pelo [run 39327380746](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39327380746), na primeira tentativa, após o PR [#17](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/pull/17). Tag/código testado `703be5998e2a4faaa12bc40c65c0a40b99b1b972` (merge em main). Conferidos os 12 assets e os 11 checksums contra digests públicos; v0.4.15 é a latest; as 20 releases anteriores mantêm arquivos, tamanhos e digests; links do README respondem. Relatório da CI: `Testes/teste22-2026-10-19_18-25-33-3f736ad4/log.txt`. O PR também trouxe a causa das falhas intermitentes de Maven (`Connection reset` ao baixar do Maven Central): `aplicativo/.mvn/maven.config` repete downloads (até 6 vezes) e a CI guarda as dependências em cache.

Testes locais (Linux x64, 19/10/2026): 126 Java (um novo: destaque da Área Interna, bolinha de solicitações e aviso só para a equipe; um alterado: público não vê o aviso), Go test/vet, Node da agenda (9, um novo sem o aviso) e de cadastros (4, dois novos da máscara de telefone), segredos, Testar 14/14, pacote Linux x64 e smoke aprovados. Windows, macOS e ARM64 ficam com a matriz.

---

# Fix 0.4.14 — publicada e verificada

A [0.4.14](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.14) foi publicada e verificada em 19/10/2026 pelo [run 39317656989](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39317656989). Tag/código testado `5cf993c5c34f7581a836229a08ab4f4511f1f097` (merge em main). Conferidos os 12 assets e os 11 checksums contra digests públicos; v0.4.14 é a latest; as 18 releases anteriores à 0.4.13 mantêm arquivos, tamanhos e digests; links de download do README respondem. Relatório da CI: `Testes/teste21-2026-10-19_17-14-33-8e2d6083/log.txt`. Caminho até a publicação: PR [#14](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/pull/14) (correções); duas execuções da main barradas por falhas intermitentes da CI (Maven no Ubuntu x64/Windows ARM; reinício e limpeza do servidor de teste no Windows x64), nenhuma reproduzida localmente; PR [#15](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/pull/15) passou a mostrar nome/mensagem de testes Java e linhas [ERROR] do Maven quando o job falha; PR [#16](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/pull/16) fez o `Testar.java` tentar de novo o início do servidor (até 3 vezes, só se ele encerrar ao iniciar) e a limpeza (até 10 s) no Windows. Pendência: a causa exata das falhas de Maven no Ubuntu x64/Windows ARM não foi identificada; se voltar, o diagnóstico do PR #15 mostra a mensagem.

Testes locais (Linux x64, 19/10/2026): 125 Java (dois testes alterados/novos: link de voltar na troca de senha e botão Cadastrar espaço na agenda), testes Go/vet/gofmt, Node da agenda e cadastros, segredos, Testar 14/14, pacote Linux x64 e smoke aprovados. Windows, macOS e ARM64 ficam com a matriz.

---

# Fix 0.4.13 — publicada e verificada

A [0.4.13](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.13) foi publicada e verificada em 19/10/2026 pelo [run 39243199166](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39243199166), após o PR [#11](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/pull/11) (matriz do PR no run 39241817701, verde). Tag/código testado `c3bdd011a08e80acb40143fadbe4633c7fa0080d` (merge em main). Conferidos os 12 assets e os 11 checksums contra digests públicos; v0.4.13 é a latest. As 18 releases/tags anteriores mantêm arquivos, tamanhos e digests. Relatório da CI: `Testes/teste17-2026-10-19_08-14-29-2e37f9bd/log.txt`.

Testes locais (Linux x64, 19/10/2026): 124 Java (um novo de email), 48 Go (+race, vet nos três sistemas, gofmt limpo), 17 Node (uma regressão nova da agenda que falha no script anterior), segredos, Testar 14/14, pacote Linux x64 e smoke (15 logins, troca de iniciador, recuperação de órfão, recusa de host externo) aprovados. Windows, macOS e ARM64 ficam com a matriz.

---

# Fix 0.4.12 — publicada e verificada

A [0.4.12](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.12) foi publicada e verificada em 19/10/2026 pelo [run 39192696646](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39192696646), após o PR [#9](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/pull/9) (matriz do PR no run 39191819341, verde). Tag/código testado `f9e7ea1f831981d1dcb92637eac8808257806b18` (merge em main). Seis alvos Windows/macOS/Linux x64/ARM64, fonte/segredos, dependências, CodeQL, relatório e publicação aprovados. Conferidos os 12 assets e os 11 checksums contra digests públicos; v0.4.12 é a latest. As 17 releases/tags anteriores mantêm arquivos, tamanhos e digests. Relatório da CI: `Testes/teste15-2026-10-19_00-01-36-b40967e0/log.txt`.

## Validação local (Linux x64)

Execução em 19/10/2026 numa cópia descartável da árvore, com JDK 21.0.12, Go 1.27.2, Maven local e Node 22.22.

- 123 testes Java (dois novos do filtro de host), 50 Go (6 novos) também com detector de concorrência, `go vet` e compilação de iniciador/testes para os seis alvos, 16 Node, check de segredos, links da documentação e `Testar.java` com 19 verificações aprovadas.
- Antes da correção, falharam no código 0.4.11: remoção de downloads interrompidos, aviso repetido de envio (3 eventos em 3 tentativas) e cliente com prazo total de 5 min. No pacote 0.4.11, `Host: atacante.example` recebia HTTP 200.
- Novos testes cobrem download lento mas ativo (mais longo que o prazo de travamento) concluído, download parado cancelado sem bloquear a versão instalada e sem repetição no Iniciar, cache da consulta de releases e aviso gráfico somente fora do modo `--headless`.
- Pacote Linux x64 gerado com `--compress=zip-6` e smoke aprovado sem locale, com pasta acentuada, recuperação de órfão e recusa de host externo (HTTP 400).

Não executados localmente: Windows, macOS, ARM64, janelas de aviso nativas (sem ambiente gráfico), CodeQL, gitleaks e OSV. A matriz de main, aprovada, é a referência para os seis alvos.

Validações anteriores: [historico/VALIDACAO-ENTREGAS.md](historico/VALIDACAO-ENTREGAS.md).

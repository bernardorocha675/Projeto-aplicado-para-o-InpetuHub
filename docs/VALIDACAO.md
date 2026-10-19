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

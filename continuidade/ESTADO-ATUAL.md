# Lote 2 desktop — 0.4.12 publicada

Em 19/10/2026 o responsável autorizou o segundo lote da análise desktop e sua publicação: download lento, erros invisíveis antes da tela, limpeza da documentação e validação das suspeitas pendentes. Base: main após a 0.4.11. Detalhes em docs/HANDOVER.md, docs/VALIDACAO.md e docs/REVISAO-DESKTOP.md.

A [0.4.12](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.12) foi publicada e verificada em 19/10/2026 pelo [run 39192696646](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39192696646), após o PR [#9](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/pull/9) (matriz do PR no run 39191819341, verde). Tag/código testado `f9e7ea1f831981d1dcb92637eac8808257806b18` (merge em main). Seis alvos Windows/macOS/Linux x64/ARM64, fonte/segredos, dependências, CodeQL, relatório e publicação aprovados. Conferidos os 12 assets e os 11 checksums contra digests públicos; v0.4.12 é a latest. As 17 releases/tags anteriores mantêm arquivos, tamanhos e digests. Relatório da CI: `Testes/teste15-2026-10-19_00-01-36-b40967e0/log.txt`.

Evidência local: 123 Java, 50 Go (+race, vet e compilação para os seis alvos), 16 Node, segredos, links, Testar 19/19 e smoke do pacote Linux x64 (sem locale, pasta acentuada, recuperação de órfão, recusa de host externo). Janelas de aviso nativas não foram vistas em tela (sem ambiente gráfico); a matriz não as exercita.

Pendências reais: decisões do responsável sobre limpeza de backups antigos, releases imutáveis no GitHub e o backup criptografado da assinatura Android; melhorias opcionais (govulncheck/JRE na CI) e suspeitas mantidas, listadas em docs/REVISAO-DESKTOP.md. Envio dos PCs continua desativado; Render exige confirmação explícita do workspace. **Entrega concluída: parar e aguardar nova autorização.**

Preservado: dados, reservas, contas, chave e backups na família 0.4.x; sem migration, reset, dependências, Render, SMTP ou Android. Histórico dos estados anteriores: [HISTORICO.md](HISTORICO.md).

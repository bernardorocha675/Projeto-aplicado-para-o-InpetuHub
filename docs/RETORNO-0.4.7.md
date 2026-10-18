# Retorno à 0.4.7 — entrega 0.4.10

Em 18/10/2026, após as entregas 0.4.8/0.4.9, o responsável pediu: “volte para a versão 0.4.7”. A captura mostra o destino externo /sobre/ exibindo página não encontrada. A alteração institucional adicionou esse link sem confirmação do destino. O retorno retira todo o redesenho posterior, em vez de remendar os links do site.

Base limpa de trabalho: c0e08b49cd0c007dc659c4949f5483a25f62ada8, branch work/main. Referência restaurada: tag v0.4.7, commit 992168e3ceb39f196b2153aa5e291fbae0e2f196. Lidos instruções, continuidade, handover e RELEASES; AGENTS.md ausente.

## Escopo do retorno

Os três componentes ativos foram restaurados a partir da tag. Os fontes de produção Java/Go/Node, templates, estilos, JavaScript e recursos são os da 0.4.7, exceto pelos identificadores da nova versão. Removidos CSS/fontes institucionais e fragmentos exclusivos da 0.4.9. Os scripts/testes da agenda retornam aos contratos da 0.4.7; duas regressões de navegação local e verificações no smoke do pacote complementam os testes existentes. Não foram alteradas regras de negócio, schema, migrations, permissões ou dependências.

A infraestrutura de Actions mantém os três pins já aprovados, JDK 21, Go 1.27.2 e todos os gates. Relatórios, documentos de entregas anteriores, histórico, tags, releases e assets são preservados. Os guias de uso/teste voltam às instruções da 0.4.7. As notas das interfaces descartadas são identificadas como históricas.

## Versão e dados

A restauração é distribuída como 0.4.10 porque o atualizador só aceita fixes superiores da mesma família. Publicar novamente como 0.4.7 não restauraria instalações 0.4.9 automaticamente e substituiria uma entrega histórica. Não se move nenhuma tag nem se troca arquivo publicado.

Dados ficam no mesmo diretório InPETU-Hub/0.4. A restauração não copia bancos para o Git, redefine contas ou elimina backups/chaves. Validações devem usar apenas instalações descartáveis e cópia de dados sintéticos de teste. Os fontes de negócio/persistência não sofreram alteração entre 0.4.7 e 0.4.9.

## Publicação, validação e limites

A [0.4.10](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.10) foi publicada e verificada em 18/10/2026 pelo [run 39160916469](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39160916469), com os 11 jobs obrigatórios aprovados. Tag/código testado `0a67211d7ea2be3834c296b3a4660d01fe5abf73`. Seis alvos Windows/macOS/Linux x64/ARM64 passaram testes, HTTP/backup, empacotamento e smoke do pacote com Java privado. Conferidos os 12 assets e os 11 checksums contra digests públicos. ZIP de código baixado e fontes de produção comparados à tag 0.4.7: idênticos, exceto identificadores de versão. As 15 releases/tags anteriores mantêm links, arquivos e digests, inclusive a 0.4.7 original e Android experimental. Relatório da CI: `Testes/teste13-2026-10-18_16-55-03-aea7918d/log.txt`.

Passaram 119 testes Java (117 da 0.4.7 e duas regressões de navegação local), 33 Go com detector de concorrência/go vet, 16 Node, check de segredos, roteiro HTTP/permissões/persistência/backup e smoke Linux. Chromium 154 aprovou navegação original, login, painel/menus, cotas/auditoria/senha, logout, reserva e larguras de 390 a 1919px, sem erros JavaScript. Cópia isolada de dados sintéticos da 0.4.9 abriu na restauração e manteve oito contagens, histórico Flyway e chave após navegação/login. JAR sem CSS/fontes descartados. A matriz comprova os seis runners previstos, não todos os sistemas/navegadores possíveis.

O pacote usa 0.4.10 para alcançar instalações 0.4.8/0.4.9 pelo atualizador; o aplicativo restaurado é o da 0.4.7. Não houve reset, migrations, mudança de contas/senhas, dependências, permissões ou regras de negócio. Dados/chave/backups permanecem no mesmo diretório da família 0.4.x; nenhum dado privado foi publicado. Android segue retirado; SMTP, Render e envio externo continuam desativados. A reprodução do site oficial foi cancelada por este pedido. Não reintroduzir alterações de interface posteriores sem autorização. O retorno integral também recupera o tratamento de falha de consulta da agenda da 0.4.7; o fix posterior da 0.4.8 não foi reaplicado. O ZIP preserva o snapshot anterior ao relatório/fechamento documental. Entrega concluída: parar e aguardar nova autorização.

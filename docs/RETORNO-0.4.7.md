# Retorno à 0.4.7 — entrega 0.4.10

Em 18/10/2026, após as entregas 0.4.8/0.4.9, o responsável pediu: “volte para a versão 0.4.7”. A captura mostra o destino externo /sobre/ exibindo página não encontrada. A alteração institucional adicionou esse link sem confirmação do destino. O retorno retira todo o redesenho posterior, em vez de remendar os links do site.

Base limpa de trabalho: c0e08b49cd0c007dc659c4949f5483a25f62ada8, branch work/main. Referência restaurada: tag v0.4.7, commit 992168e3ceb39f196b2153aa5e291fbae0e2f196. Lidos instruções, continuidade, handover e RELEASES; AGENTS.md ausente.

## Escopo do retorno

Os três componentes ativos foram restaurados a partir da tag. Os fontes de produção Java/Go/Node, templates, estilos, JavaScript e recursos são os da 0.4.7, exceto pelos identificadores da nova versão. Removidos CSS/fontes institucionais e fragmentos exclusivos da 0.4.9. Os scripts/testes da agenda retornam aos contratos da 0.4.7; duas regressões de navegação local e verificações no smoke do pacote complementam os testes existentes. Não foram alteradas regras de negócio, schema, migrations, permissões ou dependências.

A infraestrutura de Actions mantém os três pins já aprovados, JDK 21, Go 1.27.2 e todos os gates. Relatórios, documentos de entregas anteriores, histórico, tags, releases e assets são preservados. Os guias de uso/teste voltam às instruções da 0.4.7. As notas das interfaces descartadas são identificadas como históricas.

## Versão e dados

A restauração é distribuída como 0.4.10 porque o atualizador só aceita fixes superiores da mesma família. Publicar novamente como 0.4.7 não restauraria instalações 0.4.9 automaticamente e substituiria uma entrega histórica. Não se move nenhuma tag nem se troca arquivo publicado.

Dados ficam no mesmo diretório InPETU-Hub/0.4. A restauração não copia bancos para o Git, redefine contas ou elimina backups/chaves. Validações devem usar apenas instalações descartáveis e cópia de dados sintéticos de teste. Os fontes de negócio/persistência não sofreram alteração entre 0.4.7 e 0.4.9.

## Validação e limites

Passaram localmente 119 testes Java (117 da 0.4.7 mais duas regressões de navegação), 33 Go com detector de concorrência/go vet, 16 Node, check de segredos, roteiro HTTP/permissões/persistência/backup e empacotamento/smoke Linux x64 com Java privado. Chromium 154 verificou menu original, login, painel e oito menus, cotas/auditoria/senha, logout, fluxo de reserva e larguras de 390 a 1919px, sem erros JavaScript nem requisições externas no roteiro de navegação. Cópia isolada de dados sintéticos da 0.4.9 abriu na restauração: oito contagens, chave e histórico Flyway preservados após navegação e login. O JAR não contém CSS/fontes retirados. Paridade com a tag comprovada: somente versões e testes adicionais diferem nos três componentes. Outros cinco alvos aguardam a matriz nativa da CI. Publicação depende dos 11 jobs obrigatórios. Resultados finais em VALIDACAO.md e continuidade/ESTADO-ATUAL.md.

Este retorno recupera integralmente o comportamento da agenda da 0.4.7, incluindo o tratamento de falha de consulta anterior ao fix da 0.4.8. Não reaplicar alterações posteriores sem nova autorização. A reprodução do site e a conferência de seus destinos deixam de ser objetivo ativo; não há dependência do site oficial para usar o aplicativo.

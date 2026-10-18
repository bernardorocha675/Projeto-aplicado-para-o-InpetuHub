# Restauração da navegação do aplicativo — 0.4.9

O responsável apontou que a entrega 0.4.8 tinha tirado o foco da área interna e pediu seu projeto de volta, mantendo o visual institucional que aprovou. Base limpa `e8ca2336dc1ef039ebb8a47d25069f501bcd7d4d`, branch work, main no mesmo SHA. AGENTS.md ausente; instruções, continuidade, handover e RELEASES.md lidos. Última release 0.4.8 com seus 11 jobs aprovados.

## Causas confirmadas

Em `templates/fragments.html`, a 0.4.8 retirou o acesso /admin do cabeçalho público e o deixou no rodapé. Logo e Início passaram a direcionar para o site externo. Impacto: a entrada principal no sistema de gestão ficou escondida e a navegação podia tirar o usuário do aplicativo.

As telas internas usavam esse mesmo fragmento, que passou a mostrar o menu institucional no lugar da navegação do aplicativo. Em `static/css/institucional.css`, os seletores gerais de topo/rodapé também atingiam login e gestão, substituindo o layout anterior. As rotas, controladores, dados e regras continuavam presentes; a regressão era de acesso, navegação e apresentação.

## Correção

O cabeçalho público mantém a identidade institucional e oferece Área interna (/admin) e Solicitar reserva (/reserva). Logo e Início voltam para a raiz local. Os templates de administração e autenticação usam topoInterno/rodapeInterno com navegação local e barra lateral existente. As regras visuais institucionais são limitadas às classes do cabeçalho/rodapé públicos.

Dois testes de integração verificam entradas públicas e, após login real, painel, reservas, solicitações, empreendedores, espaços, atendimentos, visitas, administração, cotas, auditoria e alteração de senha. O smoke nativo valida os links no topo e os menus de gestão no JAR/runtime instalado. Os controles de autenticação, perfis e CSRF foram preservados; não há atalhos de permissão ou dados de demonstração inseridos nos bancos dos usuários.

## Publicação e validação

A [0.4.9](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.9) foi publicada e verificada em 18/10/2026 pelo [run 39159176375](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39159176375), com os 11 jobs obrigatórios aprovados. Tag/código testado `d0aeb5f364145f99b4804ff2ce8b6578b9f2313c`. Os seis alvos Windows/macOS/Linux x64/ARM64 executaram testes, roteiro HTTP/backup, empacotamento e smoke do runtime privado, incluindo os acessos restaurados. Conferidos os 12 assets, os 11 checksums contra digests públicos e o ZIP de código baixado. As 14 releases/tags anteriores mantêm links, arquivos e digests, inclusive Android experimental. Relatório de CI: `Testes/teste12-2026-10-18_16-31-49-b1b838ba/log.txt`.

Passaram 120 testes Java (dois novos), 33 Go com detector de concorrência/go vet, 19 Node, roteiro HTTP/permissões/persistência/backup, check local de segredos e smoke Linux x64. Chromium 154 verificou login, painel e menus, cotas/auditoria/senha, logout e reserva em larguras de 390 a 1919px, sem erros JavaScript. Fonte/segredos, dependências, CodeQL, relatório e publicação passaram na nova matriz. A CI comprova os seis runners previstos, sem afirmar teste em todas as distribuições ou navegadores.

Sem reset, migrations, alteração de contas/senhas, permissões, reservas, preços, cotas ou dependências. Android continua retirado; SMTP, Render e envio externo permanecem desativados. A conferência externa da marca/fontes/submenus/links oficiais continua pendente pelo bloqueio HTTP 403 já documentado; não impede a navegação local. O ZIP de código preserva o snapshot anterior ao relatório e fechamento documental; não substituir assets nem mover tags. Entrega concluída: aguardar nova autorização para outro lote.

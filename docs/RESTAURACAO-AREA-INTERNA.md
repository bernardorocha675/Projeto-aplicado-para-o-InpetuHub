# Restauração da navegação do aplicativo — 0.4.9

O responsável apontou que a entrega 0.4.8 tinha tirado o foco da área interna e pediu seu projeto de volta, mantendo o visual institucional que aprovou. Base limpa `e8ca2336dc1ef039ebb8a47d25069f501bcd7d4d`, branch work, main no mesmo SHA. AGENTS.md ausente; instruções, continuidade, handover e RELEASES.md lidos. Última release 0.4.8 com seus 11 jobs aprovados.

## Causas confirmadas

Em `templates/fragments.html`, a 0.4.8 retirou o acesso /admin do cabeçalho público e o deixou no rodapé. Logo e Início passaram a direcionar para o site externo. Impacto: a entrada principal no sistema de gestão ficou escondida e a navegação podia tirar o usuário do aplicativo.

As telas internas usavam esse mesmo fragmento, que passou a mostrar o menu institucional no lugar da navegação do aplicativo. Em `static/css/institucional.css`, os seletores gerais de topo/rodapé também atingiam login e gestão, substituindo o layout anterior. As rotas, controladores, dados e regras continuavam presentes; a regressão era de acesso, navegação e apresentação.

## Correção

O cabeçalho público mantém a identidade institucional e oferece Área interna (/admin) e Solicitar reserva (/reserva). Logo e Início voltam para a raiz local. Os templates de administração e autenticação usam topoInterno/rodapeInterno com navegação local e barra lateral existente. As regras visuais institucionais são limitadas às classes do cabeçalho/rodapé públicos.

Dois testes de integração verificam entradas públicas e, após login real, painel, reservas, solicitações, empreendedores, espaços, atendimentos, visitas, administração, cotas, auditoria e alteração de senha. O smoke nativo valida os links no topo e os menus de gestão no JAR/runtime instalado. Os controles de autenticação, perfis e CSRF foram preservados; não há atalhos de permissão ou dados de demonstração inseridos nos bancos dos usuários.

## Limites e validação

A versão aguarda a matriz obrigatória para publicação. Passaram localmente 120 testes Java (dois novos), 33 Go com detector de concorrência/go vet, 19 Node, check de segredos, roteiro HTTP/permissões/persistência/backup e empacotamento/smoke do Linux x64 com Java privado. Chromium 154 aprovou agenda → acesso interno → login → painel e oito menus, cotas/auditoria/senha, logout e fluxo completo de reserva, em larguras de 390 a 1919px, sem erros JS ou requisições externas no roteiro de navegação. A execução nativa dos outros cinco alvos depende da nova CI. Resultados em VALIDACAO.md e continuidade/ESTADO-ATUAL.md. A verificação usa dados descartáveis, sem reset de dados reais, novas dependências ou ativação de serviços externos. As fontes e o visual do painel público permanecem. O bloqueio de consulta ao domínio institucional não impede esta correção de navegação local.

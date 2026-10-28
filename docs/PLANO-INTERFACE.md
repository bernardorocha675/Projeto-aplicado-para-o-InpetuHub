# Plano de interface — visual do site oficial e acessibilidade

Plano aprovado pelo responsável em 26/10/2026. Cada lote é uma branch, um PR e uma versão 0.5.x. O próximo lote só começa depois do merge do anterior e de um novo ok. A VERSION é alterada pelo responsável na publicação.

Decisões que valem para todos os lotes:

1. **Contraste legível com a menor mudança possível:** cabeçalho branco, faixa `#0077cc` e cinza `#5f6b77`, tudo em WCAG AA.
2. **Rodapé:** YouTube, porque o site oficial aponta para `youtube.com/@InPETUhub`.
3. **Área Interna:** texto de 16 px.

Regras de cada PR:

- teste que falha antes da mudança;
- `/verificar-tudo`;
- capturas de antes e depois em 1440 e 390 px, fora do repositório (`~/.cache/inpetu-hub-capturas/loteN/`);
- axe sem violações de contraste nas telas mexidas;
- sem mexer em dados nem na CSP e sem usar CDN.

| Lote | Versão | Conteúdo | Situação |
|---|---|---|---|
| 0 | — | Templates e CSS formatados, sem mudar a tela | Feito (PR #37) |
| 1 | 0.5.9 | Agenda legível, setas de 44 px, link para pular o menu | Publicado (PR #39) |
| 2 | 0.5.10 | Pedido de reserva no estilo do contato oficial, erros ligados aos campos, preenchimento automático | Publicado (PR #41) |
| 3 | 0.5.11 | Menu hambúrguer no celular e toques de 44 px | Em revisão |
| 4 | 0.5.12 | Paleta única e componentes oficiais na Área Interna | Preparado (abaixo) |
| 5 | 0.5.13 | Tabelas no celular (foco, título, linhas em blocos) | A fazer |
| 6 | 0.5.14 | Detalhes do início (tamanho das imagens, menos movimento) | A fazer |
| — | — | Arquitetura, com plano próprio, depois de todos os lotes | A fazer |

## Lote 4 preparado — Área Interna

Os itens abaixo foram medidos em 27/10/2026, sobre a 0.5.9 e o lote 2. Cada um traz a correção proposta e o teste que deve falhar primeiro. As funções `Css.regra`, `Css.propriedade`, `Css.variavel` e `Css.assertContraste` (em `aplicativo/src/test/java/br/com/inpetu/hub/Css.java`) já calculam o contraste a partir do CSS.

| Achado | Medida hoje | Proposta | Teste primeiro |
|---|---|---|---|
| Contorno de foco `#ffb14d` (`style.css`, `:focus-visible`), em todas as telas | 1,8:1 sobre branco (o WCAG 2.4.11/1.4.11 pede 3:1) | `#1c1c1c`, o texto do site oficial: 17:1 sobre branco, 15,9:1 sobre `#f4f7fa` e 3,66:1 sobre a faixa `#0077cc`. O marinho `#02569a` daria só 1,6:1 sobre a faixa. | Contraste ≥ 3 da cor do `outline` contra branco, `#f4f7fa` e `--blue` |
| Item atual do menu interno (`admin.css`, `.sidebar nav a.current`), azul sobre `#e8f3fd` | 4,14:1 | Texto `#02569a` (marinho do site): 6,67:1 | Contraste ≥ 4,5 da regra `.current` |
| Sobretítulos da Área Interna (`.eyebrow.blue` sobre o fundo `#f4f7fa`) | 4,33:1 | `#02569a`: 6,97:1. Ou retirar o sobretítulo, como já foi feito no pedido de reserva | Contraste ≥ 4,5 de `.blue` sobre `--bg` |
| Borda dos campos internos (`style.css`, `input, select, textarea`) | `#cfdbe6`, 1,41:1 sobre branco | A mesma variável do formulário público, `--site-borda-campo` (`#8592a0`, 3,17:1), com altura de 48 px, raio de 12 px e fundo `--site-fundo-campo` | Contraste ≥ 3 da borda; altura e raio iguais aos do componente |
| Duas paletas: `--blue`/`--muted`/`--ink` em `style.css` e `--site-*` em `site.css` | `--ink` `#213345` × texto oficial `#1c1c1c`; azul repetido nos dois arquivos | Um único `:root` com os tokens oficiais; `style.css` só os referencia | Nenhum hexadecimal fora da lista de tokens nos CSS (impede a volta de `#087fc5`) |
| Alvos de toque do conteúdo interno a 390 px (medidos no lote 3) | Caixas de marcar com 13 × 13 (ativo, força maior, objetivos da visita) e 8 × 13 (confirmação de identidade em Solicitações); `summary` de Cancelar/Recusar com 26 px; Limpar dos filtros com 26 px; Editar/Perfil nas tabelas com 22 px (estes vão com o lote 5); selects com 43 px | Caixas de marcar com 24 px e linha de 44 px, como no pedido público; `summary`, botões e selects com 44 px abaixo de 980 px | Medidas lidas do CSS (`Css.media`) e conferência com `~/.cache/inpetu-hub-capturas/ferramentas/alvos_toque.py` |
| Botões, cards e avisos da Área Interna | Botão com raio de 9 px e sem sombra; card com raio de 20 px | Botão com 46 px, raio de 8 px e a sombra do botão do início oficial; card com raio de 12 px e padding de 24 px; aviso com borda esquerda de 4 px e `role="status"` (erro com `role="alert"`, num `div` e não num `ul`, como no lote 2) | Medidas lidas do CSS e `role` nos templates dos avisos |

Telas para as capturas e o axe:

- painel, solicitações, reservas e detalhe da reserva;
- listas e formulários de todos os cadastros;
- perfil, auditoria, cotas, configuração, login e troca de senha.

O script `~/.cache/inpetu-hub-capturas/ferramentas/rodada.sh` já captura todas essas telas.

Achados próximos, fora do visual. Ficam fora do lote 4, salvo novo pedido:

- A ordem das opções de Percentual (preços), Status (visitas) e Perfil (contas) muda a cada inicialização, porque vem de `Map.of` em `CrudService`. A correção é um mapa com ordem fixa e um teste da ordem.
- O perfil do empreendedor mostra "null / null" em Segmento/cargo vazios.
- No pedido de reserva, o erro do número do documento repete o rótulo ("Número do documento: Documento: informe…"), porque a mensagem de `Documentos` já começa com "Documento:".

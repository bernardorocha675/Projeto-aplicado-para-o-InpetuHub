# Agenda institucional — 0.4.8

A [0.4.8](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.8) foi publicada e verificada em 18/10/2026, exclusivamente pelo [run 39157060612](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39157060612), com os 11 jobs obrigatórios aprovados. Tag/código testado `2de098c6099af08d1fd44963978ccef94ce14adc`. Os seis alvos nativos executaram testes, roteiro HTTP/backup, empacotamento e smoke com Java privado. Conferidos os 12 assets e os 11 checksums contra digests públicos; ZIP de código baixado e verificado, incluindo quatro fontes/licenças e os SHAs oficiais das três ações. As 13 releases/tags anteriores mantêm links, arquivos e digests; nenhum asset histórico foi substituído. Relatório de CI: `Testes/teste11-2026-10-18_16-04-21-0ced77a7/log.txt`.

Pedido de 18/10/2026: o responsável informou que este é o projeto aplicado oficial do InPETU Hub e pediu a mesma apresentação do site original. Referência informada: https://inpetuhub.sites.ufsc.br/agenda/, além de três capturas com agenda inicial, rodapé e evento selecionado. Base limpa `215e57d2055faeb4a6b0455436a71a9f26a9115d`, branch `work`, main remoto no mesmo commit; última release publicada 0.4.7. Nenhum AGENTS.md encontrado no workspace. Instruções, continuidade, handover e RELEASES.md consultados.

## Implementação revisável

- `templates/public/agenda.html` e `static/css/institucional.css`: textos das capturas, painel azul, proporção 39,5%/60,5%, cantos arredondados, calendário compacto e responsivo. Cabeçalho e rodapé públicos em `templates/fragments.html`. Na 0.4.9, Área interna e Solicitar reserva ficam no topo; login/gestão usam navegação própria. Veja RESTAURACAO-AREA-INTERNA.md.
- `static/js/agenda.js`: mês no formato “Outubro 2026”, marcação com ponto apenas quando há reservas aprovadas retornadas pelo servidor, destaque do dia selecionado e detalhes de título/data/horários/sala. Nenhum evento ou padrão de ocupação da captura foi inserido no banco.
- Filtros de espaço/dia, capacidades, estados acessíveis de disponibilidade, horários livres e solicitação de reserva continuam em “Consultar espaços e solicitar reserva”, abaixo do dia selecionado. Regras de dados, reserva, preços, cotas e confirmação permanecem as da 0.4.7.
- Fontes locais Lato e Montserrat com licença OFL e procedência documentada em `static/fonts/README.md`. `SecurityConfig.java` libera somente a rota de arquivos estáticos `/fonts/**` para visitantes; `SafeRoutes.java` classifica essas requisições como ASSET.

## Bug confirmado e correção

Em `static/js/agenda.js`, o catch de atualização antes só trocava a grade pelo erro. Os links e horários da consulta anterior continuavam ativos na área de detalhes. Impacto: uma falha de rede/filtro deixava ações de reserva baseadas numa consulta antiga. A validação do servidor continuava necessária e não foi removida.

Agora o erro esconde as ferramentas e limpa os links antigos, com mensagem para nova consulta. O regressor Node simula falha após uma consulta válida. Remover apenas a limpeza do catch reproduziu a falha do teste (`hidden=false`); o código corrigido passou. Isso não altera reserva nem apaga dados.

## Dependência externa e limites da comparação

A requisição ao domínio oficial foi recusada pela rede do ambiente com HTTP 403. A configuração observada não contém esse domínio. Foi salvo um rascunho aditivo com `inpetuhub.sites.ufsc.br`; salvar o rascunho não aplica nem publica a configuração. É preciso revisar/salvar em configurações do ambiente e publicar para retomar a consulta.

**Publicação autorizada em 18/10/2026.** Depois de informado sobre o bloqueio e a preparação, o responsável pediu verificar os PRs e “pode publicar tudo”. Essa autorização permite entregar a implementação pelas capturas após os gates obrigatórios; não transforma as seguintes pendências em equivalência verificada:

1. Obter/conferir o arquivo da marca e as fontes/CSS originais. A marca atual é texto estilizado; Lato/Montserrat foram escolhidas visualmente.
2. Conferir destinos de Sobre, Iniciativas, Notícias e Eventos, Contato, Política de Privacidade e Termos de Uso. Os caminhos no template são provisórios, derivados dos rótulos, ainda sem validação HTTP. Conferir e implementar os submenus reais; não há itens de submenu inventados.
3. Conferir os destinos dos ícones sociais do rodapé antes de reproduzi-los. A barra atual mantém acesso interno/site institucional.
4. Comparar navegação, fontes e interações com o site acessível. A comparação local usa as capturas, não o DOM original. Não existe sincronização com a agenda pública da UFSC: os eventos do executável vêm das reservas locais.

Escopo assumido enquanto a preferência opcional não é respondida: agenda e identidade compartilhada do aplicativo. Páginas institucionais completas não foram copiadas para o executável.

## Validação

Passaram 118 Java, 33 Go/-race/vet, 19 Node, roteiro HTTP/backup, navegador e smoke local Linux. Código testado `12bdb6b169b4020692deb88584751ac80757203a`, enviado para `release-v0.4`. [Run 39155638958](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/actions/runs/39155638958): nove jobs aprovados — Ubuntu 24.04 x64/ARM64, macOS 15 Apple Silicon/Intel, Windows Server 2025 x64, Windows 11 ARM64, fonte/segredos, dependências e CodeQL. Cada alvo nativo executou testes, roteiro HTTP/backup, empacotamento e smoke. Relatório público em Testes e publicação de release não foram executados: são restritos a main. Não confundir estes nove checks com uma release publicada ou com o pipeline de publicação completo. Evidências e limites em VALIDACAO.md e continuidade/ESTADO-ATUAL.md. O navegador usou dados sintéticos em instalação descartável; os 16 dias ocupados da captura foram simulados somente na requisição interceptada de teste visual. Nenhum dado de negócio ou serviço externo foi alterado. A comparação funcional agenda → reserva usa o JAR real e o endpoint real, separadamente dessa simulação visual.

A publicação da 0.4.8 foi autorizada com os limites acima documentados e continua condicionada aos gates de RELEASES.md. Revisão das ações da CI em REVISAO-PRS-2026-10-18.md. Releases/tags/assets anteriores, inclusive Android experimental, permanecem preservados.

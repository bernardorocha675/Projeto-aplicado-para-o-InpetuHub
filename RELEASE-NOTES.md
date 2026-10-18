# InPETU Hub 0.4.7

Corrige os itens restantes do relatório caçabugs no desktop:

- Nome sem números, email com domínio completo e telefone sem letras, no navegador e servidor; permite corrigir e reenviar sem perder o cadastro anterior.
- Data da agenda preservada na reserva, nos erros e ao voltar da revisão para editar. Reservas limitadas de hoje até 31/12 do próximo ano, com atualização automática anual.
- Confirmação local orienta conferência manual sem pedir um código inexistente. Código ativo mantém os controles de produção, expiração e tentativas.
- Agenda com consulta direta por dia, capacidades no filtro e indicação de livre/parcial/ocupado; grade estreita sem cortes e asteriscos nos campos obrigatórios.

Sem reset, migrations, mudança de contas/senhas, preços ou cotas. Solicitações históricas preservadas. Android continua fora do produto ativo, e SMTP/Render/envio externo não foram ativados. Preços futuros precisam de tabela configurada pelo gestor. O executável inclui Java privado e o atualizador permanece na família 0.4.x.

Publicação somente após os seis ambientes nativos e todos os gates de CI. Detalhes em docs/CORRECOES-RELATORIO.md.

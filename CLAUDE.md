# InPETU Hub — instruções para continuidade

Este repositório é o projeto acadêmico de Projeto Aplicado do UniSENAI, em parceria com o InPETU Hub. Comunique-se em português do Brasil, de forma clara e direta.

## Estado que precisa acompanhar cada sessão

@continuidade/ESTADO-ATUAL.md

## Antes de alterar código

1. Leia `continuidade/CONTEXTO.md` e `continuidade/OPERACAO.md`; para detalhes da entrega, consulte `docs/HANDOVER.md`.
2. Confira `git status`, `VERSION`, a release mais recente e os resultados de Actions. Este contexto tem data e pode ficar desatualizado; o código e as evidências atuais prevalecem sobre o resumo.
3. Trabalhe nos fontes ativos: `aplicativo/` (Java), `iniciador/` (Go) e `receptor/` (Node). Os guias ficam em `docs/`. Versões antigas estão nas tags/releases e no backup; não recrie pastas de versões na raiz.
4. Preserve mudanças de outras pessoas e limite as alterações ao pedido. Quando o pedido for somente analisar, não edite, faça commit, merge ou deploy.

## Decisões que devem ser preservadas

- O usuário final usa o executável; VS Code, Maven, Go e Java externo são ferramentas de desenvolvimento, não requisitos de uso da release.
- Mantenha Windows, macOS e Linux, nas arquiteturas x64/ARM64. A instalação e os dados do usuário podem ter espaços e acentos; sempre use aspas nos comandos.
- Os fixes automáticos ficam na mesma família `0.4.x`; não migre automaticamente para `0.5`.
- Preserve banco, reservas, chave, ambiente e backups. Não redefina dados nem contas para corrigir um bug sem autorização específica para esse reset.
- Existem cinco contas públicas de demonstração local. A decisão de simplificar o login não autoriza remover proteções dos outros perfis. Não reintroduza MFA obrigatório na demonstração sem novo pedido.
- Logs de sessão começam automaticamente ao abrir o iniciador. Não criar um botão para iniciar teste. Incluir versões do aplicativo e do iniciador; manter a sessão durante a troca de executável.
- O time não deve precisar entrar no GitHub para enviar relatórios. Nunca embutir credenciais de escrita do GitHub no executável ou receptor.
- Publicar somente relatórios sanitizados. Não copiar chat, senhas privadas, tokens, cookies, emails, formulários, banco ou logs operacionais brutos para esta área ou para `Testes/`.
- Não confundir relatórios de CI já publicados com envio dos computadores do time: consulte a configuração real do receptor.

## Trabalho e entrega

Faça mudanças pequenas e verificáveis, execute os testes pertinentes e informe o que foi e o que não foi testado. Uma execução em um sistema não comprova todos os sistemas.

Mudanças funcionais são entregues em GitHub Releases após a matriz da CI. Não mova tags nem substitua assets publicados. Siga `RELEASES.md`; atualize o handover da linha ativa e mantenha a seção “Versão atual” do README breve. A política de segurança é voltada ao público e deve usar linguagem simples; a menção a IA permanece discreta no rodapé.

Ao concluir uma tarefa, atualize `continuidade/ESTADO-ATUAL.md` com: mudança, evidência dos testes, commit/release de referência e pendências reais. Atualize `CONTEXTO.md` apenas quando uma decisão duradoura mudar. Não marque algo como feito apenas porque foi solicitado no histórico.

As conexões do ChatGPT não são transferidas ao Claude Code. Verifique as integrações disponíveis na nova ferramenta; nunca registre credenciais nestes documentos.

A etapa de backup e a reorganização foram concluídas; a release 0.4.3 foi publicada e validada. O usuário pediu uma etapa por vez: pare e aguarde um novo pedido/autorização antes de outra tarefa. A migração de uso é exclusivamente pelo executável; comandos de código são internos ao desenvolvimento/CI.

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

Backup e reorganização foram concluídos na 0.4.3. O relatório caçabugs_0.1_Joaoe.docx.PDF foi analisado e a parte 1 (cadastro de espaços) foi concluída na release 0.4.4, publicada e verificada. Consulte docs/BUGS.md e continuidade/ESTADO-ATUAL.md. Pare e aguarde nova autorização para outra parte. O Render não está incluído nesta etapa.

O fix 0.4.5 de logs foi publicado e verificado, com seis ambientes aprovados. Consulte o estado atual e o handover. O envio dos PCs ao GitHub ainda não foi ativado: a integração Render exige identificar explicitamente o workspace, e a descoberta desta sessão não autoriza selecionar My Workspace sem a resposta do usuário. Parar para essa confirmação; não avançar automaticamente outras partes do PDF nem declarar envio real como concluído.

Pedido atual: Android experimental independente/offline, confirmado pelo usuário. Fontes em android/, guia docs/ANDROID.md e handover docs/HANDOVER-ANDROID.md. Linha Android 0.1.0-experimental.1 é separada; desktop continua 0.4.5. Não sincronizar bancos nem ativar Render por esse pedido. Publicar somente após testes em emulador. Chave privada de recuperação entregue fora do Git; manter apenas certificado público no repositório e backup criptografado na release. Futuras releases exigem reutilizar a assinatura, nunca gerar outra silenciosamente.

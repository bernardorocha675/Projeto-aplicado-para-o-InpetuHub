# InPETU Hub 0.4.1

- Corrige a agenda que mostrava todos os dias ocupados quando não havia espaços cadastrados ou habilitados para reservas.
- Exibe **Sem espaços** e orienta a equipe a cadastrar ou habilitar os espaços. Dias livres, parcialmente ocupados e lotados continuam respeitando as reservas aprovadas.
- Preserva o banco, os cadastros, as reservas e os cinco acessos da 0.4.0. Não é necessário resetar o banco.

## Atualizar

Pare o aplicativo e abra novamente o iniciador da 0.4, ou use **Configurar / atualizar**. O fix estável 0.4.1 é instalado automaticamente. Depois clique em **Iniciar** e atualize a página da agenda.

Também é possível baixar o ZIP completo do seu sistema/arquitetura, extrair e abrir InPETU-Hub.exe (Windows), InPETU Hub.app (macOS) ou InPETU-Hub (Linux). Os pacotes incluem Java; não exigem VS Code ou arquivo de senhas.

## Habilitar a agenda no primeiro uso

Na área da equipe, abra **Cadastros → Espaços → Novo cadastro**. Informe nome e capacidade, marque **Ativo** e **Disponível para reservas** e salve. Se o espaço já existir, edite o cadastro. Sem reservas aprovadas, ele aparecerá livre. O aviso da agenda inclui um link para essa tela.

Sem espaços cadastrados, não existem horários que possam ser reservados. A correção deixa essa situação explícita; não cria salas fictícias nem remove reservas reais. As contas e senhas de demonstração permanecem iguais às da 0.4.0; consulte ACESSOS.md.

Testes da agenda, do iniciador e dos pacotes integram a publicação em Windows, macOS e Linux, x64/ARM64. Consulte o handover e o guia de testes na pasta versão 0.4. Os executáveis continuam sem assinatura comercial/notarização Apple.

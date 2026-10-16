# Segurança — escopo e propriedades

Esta versão trata de contas privadas, sessões, abuso de login/solicitações, identidade declarada, cotas, auditoria, cabeçalhos HTTP, transporte, backups e verificação contínua. Não há declaração de segurança absoluta.

## Propriedades exigidas

- Leitura do repositório não revela as contas particulares nem permite autenticar em um banco provisionado com elas.
- Uma instalação sem arquivo de acessos não recebe uma senha conhecida: gera uma credencial local aleatória.
- Um cliente não autenticado não altera o CRM, não bloqueia salas nem aloca cotas ao enviar uma solicitação.
- Encaminhar ao CRM exige conferência do vínculo institucional por equipe autenticada, com registro de autoria; em produção, a confirmação de email também é obrigatória.
- Apenas reservas aprovadas ocupam horários e cotas. Aprovações concorrentes preservam a integridade sob o bloqueio transacional existente.
- Alterações sensíveis de conta tornam as sessões anteriores inválidas na requisição seguinte.
- Administradores e colaboradores autenticam com login e senha. O segundo fator foi removido em 0.3.1fix, inclusive para contas antigas; a proteção administrativa passa a depender da senha e dos controles de sessão, perfil e limitação de tentativas.
- A auditoria do aplicativo não tem endpoints de edição/exclusão e não armazena credenciais.
- Arquivos de configuração e acessos têm permissões restritas, mas administradores do sistema operacional ainda podem acessar o processo/arquivos. Não é possível prometer sigilo contra o dono do computador ou contra quem já possui todas as credenciais.

## Operação responsável

Não incluir senhas, contatos particulares, documentos pessoais, bancos, logs de requisição ou chaves em issues, commits, screenshots e artefatos de CI. Ao relatar falhas, informar o caminho e o efeito de maneira privada, sem publicar dados de terceiros.

Os limites temporários não substituem proteção de infraestrutura. CSP não substitui escape de conteúdo. Confirmação de email e validação de CNPJ não demonstram representação legal da instituição. O operador deve conferir isso com um contato conhecido.

Dados declarados e registros de CRM continuam sendo dados pessoais. O responsável pela operação precisa definir finalidade, prazo de retenção, acesso de colaboradores e tratamento de backups. Não ativar logs de corpos HTTP nem logs SQL com parâmetros reais. Os relatórios públicos registram apenas resultados sintéticos.

## Logs técnicos da versão 0.3

Eventos controlados persistem em `logs/`, fora do Git. Mensagens livres de frameworks/exceções não são serializadas; apenas tipo e até 20 frames. O HTTP registra método, status, duração e requestId gerado no servidor, sem caminho, parâmetros, corpos, IP ou cabeçalhos. A auditoria no arquivo só espelha transações confirmadas, sem detalhes livres. Pasta privada POSIX/ACL preparada antes do Spring; administradores do sistema ainda têm acesso. Retenção técnica de 14 dias/200 MB de arquivos antigos, com limpeza periódica; não se aplica às linhas de auditoria do banco. Logs não são cifrados nem invioláveis. Consulte [LOGS.md](../LOGS.md).

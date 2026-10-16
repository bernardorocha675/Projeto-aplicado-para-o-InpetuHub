# Segurança da entrega 0.4

Esta entrega usa cinco contas públicas de demonstração no perfil local, a pedido da equipe. As senhas não protegem dados confidenciais. O provisionamento de produção recusa esses valores públicos. O iniciador desktop força execução local e não publica o servidor na rede.

Permanecem BCrypt, perfis, proteção de sessão, CSRF, cabeçalhos, auditoria e logs sem valores de credenciais. O controlador de instalação exige token/origem/Host locais. Os dados ficam no perfil do usuário, separados dos pacotes de atualização. Downloads usam HTTPS e SHA-256; ZIPs são verificados contra caminhos inválidos e limites. Isso não constitui assinatura independente do repositório.

Chave, banco, configuração e backups continuam privados. Os backups automáticos não são cifrados e não devem ser compartilhados. Executáveis sem assinatura comercial/notarização podem exigir confirmação do sistema. A versão desktop é para demonstração acadêmica; implantação real exige contas privadas, revisão de riscos, backup, infraestrutura e validação próprias.

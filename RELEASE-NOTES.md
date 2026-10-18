# InPETU Hub 0.4.6

O produto ativo passa a focar os executáveis Windows, macOS e Linux, em x64/ARM64. O suporte Android foi retirado por decisão do responsável; a release experimental e o histórico permanecem preservados.

- O iniciador recusa recriar chave ou configuração ausente quando há dados existentes e orienta recuperar o conjunto original de backup. Não altera banco, contas ou senhas.
- Uma transferência de atualização interrompida mantém a versão instalada disponível. Pacotes com checksum inválido continuam recusados, sem instalação parcial.
- Fontes, recursos, testes, scripts, workflow e guias exclusivos do Android foram retirados da árvore atual.

Os pacotes incluem Java privado; o usuário não precisa de ferramentas de desenvolvimento. Atualizações permanecem limitadas à família 0.4.x. Render e envio externo continuam desativados.

Publicação condicionada aos seis ambientes nativos, testes HTTP/backup, smoke do executável e verificações de código, segredos, dependências e CodeQL no mesmo commit.

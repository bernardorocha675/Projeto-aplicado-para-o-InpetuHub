# InPETU Hub 0.4.2

- Registro automático de sessões ao abrir o executável, sem botão de iniciar teste.
- Versões do aplicativo e do iniciador em cada evento, com ações, navegação, configuração, atualização e erros.
- Relatórios locais, recuperação após interrupção e continuidade durante a troca automática de iniciador.
- Fila de envio para Testes/testeNN-data-id no GitHub, com log.txt e resumo.json; nenhum login ou token do GitHub nos computadores do time.
- Resumo automático dos testes da CI publicado em Testes antes da release.

## Usar

Atualize pelo iniciador da linha 0.4 ou baixe o ZIP completo do seu sistema/arquitetura. Abra o executável e use normalmente. Fechar iniciador finaliza a sessão; fechar só a aba mantém o processo e os registros ativos.

Os arquivos ficam em logs/sessoes na pasta de dados do usuário. O envio exige receptor online e enabled=true em Testes/configuracao.json. Enquanto estiver desativado ou indisponível, os registros ficam no computador e a interface informa a pendência. A ativação posterior não exige reinstalação. A publicação no GitHub ocorre em segundo plano e pode levar alguns minutos.

Não são enviados senhas, tokens, cookies, formulários ou banco. Os relatórios usam códigos de ações, IDs locais, tipos de erro e pontos do código. Limites e condições estão em RELATORIOS.md e Testes/README.md. CONCLUIDO representa encerramento normal; erros individuais constam no log e no resumo.

Banco, reservas, contas e senhas da 0.4.1 são preservados. Mantidos os pacotes Windows, macOS e Linux x64/ARM64 e a correção da agenda. O handover explica instalação e operação do receptor.

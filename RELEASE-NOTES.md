# InPETU Hub 0.4.4

Primeira parte do relatório de bugs: cadastro de espaços.

- Capacidade aceita somente número inteiro positivo, com validação no navegador e no servidor.
- Dados inválidos retornam ao mesmo formulário com mensagem simples, mantendo nome, capacidade e opções marcadas/desmarcadas.
- Corrigir um erro permite salvar sem precisar reabrir o cadastro; o botão não fica bloqueado após a tentativa.
- Rejeições no servidor geram evento de log CADASTRO_INVALIDO/NEGADO sem copiar os dados do formulário.
- Testes de regressão cobrem criação, edição, preservação do banco após falha e novo envio após correção.

## Usar

Baixe o pacote completo do seu sistema em Releases e abra o iniciador. Instalações 0.4.x podem atualizar pelo próprio executável. Banco, reservas, contas e configurações são preservados; não há migration ou reset.

As demais partes do relatório ainda não foram corrigidas. O acompanhamento está em docs/BUGS.md; a próxima etapa depende de nova autorização.

Os logs locais continuam automáticos. O envio do time ao GitHub permanece desativado até a configuração do receptor, como na versão anterior.

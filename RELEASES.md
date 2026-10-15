# Publicação de versões

As entregas do InPETU Hub são publicadas em [GitHub Releases](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases), com número de versão, resumo das mudanças e pacote para download.

As pastas históricas passam a se chamar `versão 0.1`, `versão 0.2` e `versão 0.3`. A entrega atual usa a tag **v0.3.0**. As entregas anteriores continuam no histórico; não foram republicadas como releases novas.

## Preparar a próxima entrega

1. Faça as alterações na pasta da versão atual e atualize seu handover e guias quando necessário.
2. Defina um número novo em `VERSION`, sem o prefixo `v`, e use o mesmo número na versão do projeto no `pom.xml` e nos caminhos do JAR em scripts/guias. Uma correção da versão atual pode ser `0.3.1`; a próxima entrega em pasta própria pode usar `versão 0.4` e `0.4.0`.
3. Reescreva `RELEASE-NOTES.md` com o número e as mudanças dessa entrega. Atualize o README e a política de segurança se a versão atual mudar. Para uma pasta nova, ajuste também os caminhos, o nome do workflow e o Dependabot em `.github/`.
4. Envie o commit para `main`. A automação da versão atual executa os testes em Linux, macOS e Windows, além das verificações de código, dependências e arquivos privados.
5. Se todas essas verificações passarem, o job **Publicar release** cria a tag e a release no commit testado. O pacote contém somente arquivos rastreados pelo Git da versão atual e a documentação pública selecionada; inclui um resumo SHA-256.

A publicação só ocorre em `main`, após as verificações. Pull requests, branches de trabalho e execuções agendadas não publicam. Uma release existente não é substituída: correções exigem outro número. Um commit sem alterar `VERSION` não gera uma nova release. Após uma falha transitória, é possível executar novamente o workflow da versão atual na aba Actions em `main`; a publicação continua dependendo dos testes.

## Atualizar uma instalação

Faça um backup e consulte o handover antes de substituir o código. Renomear uma pasta não transfere o banco nem a configuração de uma instalação para outra. Preserve os arquivos privados e a chave MFA existentes. As contas são entregues por canal privado e nunca acompanham a release pública.

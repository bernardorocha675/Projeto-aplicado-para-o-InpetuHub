# Publicação de versões

A entrega atual é **0.4.0**, na pasta versão 0.4. Entregas anteriores permanecem no histórico. Novas entregas são publicadas em GitHub Releases depois dos testes.

Para um fix 0.4.x, atualize VERSION, a versão no POM e caminhos do JAR dos scripts de desenvolvimento; reescreva RELEASE-NOTES.md e os guias afetados. Envie para uma branch de trabalho e revise os resultados. Em main, o workflow da 0.4 compila seis pacotes nativos, executa o smoke do próprio executável/runtime e as verificações de código/dependências/segredos antes de publicar.

O publicador só aceita os artefatos do mesmo run e inclui SHA256SUMS.txt e o código público. Uma release publicada não é substituída; correções exigem novo número. Nenhuma chave, configuração, banco ou backup de usuário acompanha os pacotes. As cinco credenciais de demonstração da 0.4 são intencionalmente públicas.

O iniciador instala fixes da própria linha 0.4.x, preservando os dados do perfil. As pastas da 0.3 não são apagadas/importadas. A geração atual usa Go 1.27.2 e JDK 21 para construir um runtime privado; atualizações dessas ferramentas exigem revisão dos pins/build e uma nova release. Os workflows históricos da 0.3 continuam verificando seu código, mas não publicam a 0.4.

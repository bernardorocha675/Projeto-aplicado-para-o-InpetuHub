# Publicação de versões

A linha atual usa executáveis nativos. Os fontes estão em `aplicativo/`, `iniciador/` e `receptor/`; a documentação está em `docs/`. As releases anteriores e suas tags permanecem disponíveis. A reorganização é a entrega **0.4.3**.

## Preparar e validar

Para um fix 0.4.x, atualize VERSION, a versão no POM, buildVersion de referência no iniciador, a versão do receptor e os caminhos do JAR e a identificação da versão no roteiro de testes. Reescreva RELEASE-NOTES.md e atualize handover, guias afetados e continuidade.

Em main, `.github/workflows/release-v0.4.yml` compila e testa os seis pacotes nativos, executa o roteiro HTTP/backup e o smoke do próprio executável/runtime. Código, segredos, dependências, CodeQL e publicação do relatório de CI são etapas obrigatórias antes da publicação. O resultado concreto está no run associado ao commit da tag.

O Maven de CI é preparado por `.github/scripts/setup_maven.py`, com SHA-512 oficial. Go 1.27.2 e JDK 21 continuam fixados no workflow para construir o iniciador e o Java privado. Alterações nessas ferramentas exigem revisão e uma nova release.

## Artefatos e preservação

O publicador só aceita os artefatos do mesmo run: seis ZIPs completos, dois EXEs, dois RUNs, ZIP do código público organizado e SHA256SUMS.txt. O ZIP de código inclui guias, continuidade e arquivos de CI necessários à reconstrução. Nenhum banco, chave, configuração local ou backup de usuário acompanha os pacotes. As cinco credenciais de demonstração são intencionalmente públicas.

Uma release publicada não é substituída. Correções exigem novo número; não mover tags, excluir releases antigas ou trocar assets para reorganizar o repositório. Os fontes antigos podem ser consultados nas tags; sua retirada da árvore principal não apaga o histórico.

O iniciador instala somente fixes estáveis da própria família 0.4.x, preservando dados no perfil do usuário. A reorganização do código não muda a pasta `InPETU-Hub/0.4`, não redefine contas e não importa instalações da 0.3. Detalhes em [docs/ATUALIZACOES.md](docs/ATUALIZACOES.md).

## Linha Android experimental

A linha Android é independente da versão desktop. Versão em `android/VERSION`, fontes em `android/` e workflow `.github/workflows/android-experimental.yml`. A primeira tag é `android-v0.1.0-experimental.1`; publicar como prerelease com `latest=false`, após compilação, verificação da assinatura e testes em emulador. Não alterar VERSION/POM/iniciador da linha desktop para publicar Android. Reutilizar a assinatura nas próximas versões, configurar os secrets de manutenção e consultar `docs/HANDOVER-ANDROID.md`. O publicador não substitui uma release existente.

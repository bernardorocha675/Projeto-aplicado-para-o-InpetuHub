# Instalar e iniciar — 0.4

## Escolher o download

Abra a [release mais recente](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/latest) e escolha o pacote completo do seu sistema; `<versão>` é o número exibido na release, por exemplo 0.4.12. Os arquivos de código-fonte são para desenvolvimento.

| Computador | Pacote |
|---|---|
| Windows Intel/AMD | InPETU-Hub-Windows-x64-v<versão>.zip |
| Windows ARM | InPETU-Hub-Windows-ARM64-v<versão>.zip |
| Mac com Apple Silicon (M1 ou mais recente) | InPETU-Hub-macOS-ARM64-v<versão>.zip |
| Mac Intel | InPETU-Hub-macOS-x64-v<versão>.zip |
| Linux Intel/AMD | InPETU-Hub-Linux-x64-v<versão>.zip |
| Linux ARM64 | InPETU-Hub-Linux-ARM64-v<versão>.zip |

Os pacotes completos já incluem Java e o aplicativo. Você também pode baixar somente o iniciador `.exe` no Windows ou `.run` no Linux; nesse caso, a primeira instalação precisa de internet para baixar o pacote completo.

## Windows

Extraia o ZIP inteiro e dê dois cliques em **InPETU-Hub.exe**. Não abra arquivos de dentro do ZIP sem extrair. Se usar apenas o EXE avulso, abra-o e aguarde o download. Não é necessário executar como administrador.

## macOS

Requer macOS 12 Monterey ou mais recente. Extraia o ZIP e abra **InPETU Hub.app**. Você pode movê-lo para Aplicativos. Esta entrega não tem notarização da Apple: se o macOS bloquear, confira a origem do download e clique no app com o botão direito (ou Control + clique) → Abrir, ou permita a abertura em Ajustes do Sistema → Privacidade e Segurança → Abrir Mesmo Assim (no Monterey: Preferências do Sistema → Segurança e Privacidade → Geral). Políticas de computadores administrados podem exigir liberação do responsável.

## Linux

Extraia o ZIP. Nas propriedades de **InPETU-Hub**, permita a execução e abra-o. Se o gerenciador de arquivos não iniciar executáveis, abra um terminal nessa pasta e execute:

```sh
chmod +x InPETU-Hub
./InPETU-Hub
```

Para o iniciador avulso `.run`, use o nome completo baixado no lugar de `InPETU-Hub`. É necessário um ambiente desktop com navegador padrão; os pacotes foram preparados para x64 e ARM64.

## Usar

1. O iniciador abre uma tela no navegador e prepara o aplicativo. Aguarde a indicação **Pronto**.
2. Clique em **Iniciar**. O sistema abre em uma nova aba.
3. Entre com um dos acessos de [ACESSOS.md](ACESSOS.md), também mostrados no iniciador.
4. Para parar, volte ao iniciador e clique em **Parar**. **Fechar iniciador** encerra o sistema e o iniciador.

Fechar apenas a aba não encerra o aplicativo. Abra novamente o executável para voltar ao iniciador já aberto. Não execute a mesma pasta de dados por dois iniciadores diferentes.

Windows pode mostrar SmartScreen porque o EXE não tem certificado comercial. Confira se o download veio deste repositório; em computadores gerenciados, consulte o responsável. Não desative as proteções do sistema.

## Primeiro cadastro de espaços

O banco novo começa sem espaços cadastrados. A agenda mostra **Sem espaços**, que é diferente de um dia ocupado por reservas. Para habilitar a agenda:

1. Entre na área da equipe e abra **Cadastros → Espaços** (ou use o link **Gerenciar espaços** no aviso da agenda).
2. Clique em **Novo cadastro**, preencha o nome e a capacidade e marque **Ativo** e **Disponível para reservas**.
3. Salve e volte à agenda. Sem reservas aprovadas, o espaço aparecerá livre; a consulta é atualizada automaticamente em até 30 segundos.

Espaços já existentes podem ser editados nessa mesma tela. Não é preciso apagar ou recriar o banco.

## Dados e contas

A instalação usa a pasta `InPETU-Hub/0.4` dentro da configuração do seu usuário: LocalAppData no Windows, Library/Application Support no macOS e normalmente `~/.config` no Linux. O banco continua disponível nos próximos reinícios. Cada computador tem seu próprio banco.

Não é necessário copiar arquivo de senhas. As cinco contas são criadas automaticamente no banco e as senhas públicas voltam aos valores publicados quando o sistema inicia. Registros de negócio novos não são apagados ao reiniciar.

A primeira execução da 0.4 começa limpa; a pasta da 0.3 permanece intacta. Não copie banco e chave manualmente sem conferir o procedimento técnico do handover.

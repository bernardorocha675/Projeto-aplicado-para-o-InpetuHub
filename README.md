# InPETU Hub CRM

Sistema de gestão de relacionamento (CRM) e de reservas de espaços feito para o InPETU Hub, como projeto aplicado do UniSENAI. Ele roda no seu computador e abre no navegador.

**[Baixar](#baixar)** · **[Como executar](#como-executar)** · **[Versão de teste 1.0](#versão-de-teste-100-alpha1)** · **[Equipe](#equipe-e-créditos)** · **[Desenvolvimento](#desenvolvimento)**

## Sobre o projeto

A equipe do Hub guardava as informações dos empreendedores em várias planilhas, sem histórico dos atendimentos e com dificuldade para organizar as reservas. O sistema reúne tudo em um só lugar:

- **Empreendedores:** cadastro, busca e um perfil com reservas, atendimentos, visitas e histórico.
- **Atendimentos e visitas:** registro ligado ao empreendedor e ao colaborador responsável.
- **Agenda pública:** qualquer pessoa consulta os dias e envia um pedido de reserva.
- **Solicitações e reservas:** a equipe aprova, recusa ou cancela.
- **Espaços e painel:** cadastro dos espaços e visão das próximas reservas.

## Baixar

### Versão oficial

**Versão 0.5.12**, com todas as funções acima. Escolha o pacote do seu computador; não é preciso instalar mais nada.

| Computador | Executável |
| --- | --- |
| Windows (Intel/AMD) | [Baixar a 0.5.12](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/download/v0.5.12/InPETU-Hub-Windows-x64-v0.5.12.zip) |
| Windows ARM | [Baixar a 0.5.12](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/download/v0.5.12/InPETU-Hub-Windows-ARM64-v0.5.12.zip) |
| Mac Apple Silicon (M1 ou mais recente) | [Baixar a 0.5.12](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/download/v0.5.12/InPETU-Hub-macOS-ARM64-v0.5.12.zip) |
| Mac Intel | [Baixar a 0.5.12](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/download/v0.5.12/InPETU-Hub-macOS-x64-v0.5.12.zip) |
| Linux (Intel/AMD) | [Baixar a 0.5.12](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/download/v0.5.12/InPETU-Hub-Linux-x64-v0.5.12.zip) |
| Linux ARM | [Baixar a 0.5.12](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/download/v0.5.12/InPETU-Hub-Linux-ARM64-v0.5.12.zip) |

No Mac, é necessário o macOS 13 Ventura ou mais recente. Para estudar o projeto, baixe o [código completo da 0.5.12](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/download/v0.5.12/InPETU-Hub-codigo-v0.5.12.zip). Esta versão se atualiza sozinha dentro da linha 0.5, mantendo os dados. Outras versões estão em [Releases](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases).

### Versão de teste 1.0.0-alpha.1

O sistema está sendo refeito, e esta é a primeira prévia. **Use só para testar, com dados fictícios.**

| Computador | Executável |
| --- | --- |
| Windows (Intel/AMD) | [Baixar a 1.0.0-alpha.1](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/download/v1.0.0-alpha.1/InPETU-Hub-Windows-x64-v1.0.0-alpha.1.zip) |
| Linux (Intel/AMD) | [Baixar a 1.0.0-alpha.1](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/download/v1.0.0-alpha.1/InPETU-Hub-Linux-x64-v1.0.0-alpha.1.zip) |

- **Já tem:** agenda pública, pedido de reserva, solicitações, reservas e cadastro de espaços.
- **Ainda não tem:** empreendedores, atendimentos, visitas, colaboradores e as telas de administração e configuração. Por enquanto, só as reservas da persona P1 podem ser aprovadas.
- **Os dados ficam separados:** ela não usa nem altera os dados da 0.5.12, e as duas podem ficar no mesmo computador.

## Como executar

Nas duas versões, **extraia o ZIP inteiro** antes de abrir.

| | Versão oficial 0.5.12 | Versão de teste 1.0.0-alpha.1 |
| --- | --- | --- |
| **Windows** | `InPETU-Hub.exe` | `inpetu-hub.exe` |
| **Mac** | `InPETU Hub.app` | não disponível |
| **Linux** | `InPETU-Hub` | `inpetu-hub` |
| **Iniciar** | O iniciador abre no navegador; espere **Pronto** e clique em **Iniciar** | O sistema inicia sozinho e abre o navegador |
| **Encerrar** | No iniciador, **Parar** ou **Fechar iniciador** | Feche a janela do iniciador |

Entre na Área Interna com um dos [acessos de demonstração](docs/ACESSOS.md).

Se o computador bloquear a abertura, confira se o arquivo veio deste repositório e:

- **Windows:** escolha *Mais informações → Executar assim mesmo*.
- **Mac:** permita em *Ajustes do Sistema → Privacidade e Segurança → Abrir Mesmo Assim*.
- **Linux:** permita a execução nas propriedades do arquivo.

Os dados ficam no seu computador. O passo a passo da versão oficial está em [Instalação e execução](docs/COMO-INICIAR.md).

## Equipe e créditos

Projeto aplicado oficial do **InPETU Hub**, desenvolvido na disciplina de **Projeto Aplicado do UniSENAI** por:

- Bernardo Rocha
- João Mussiol
- João Feitosa
- Guilherme Batista

## Desenvolvimento

| Assunto | Versão oficial (0.5) | Versão nova (1.0) |
| --- | --- | --- |
| Código | `aplicativo/`, `iniciador/` e `receptor/` | `sistema/`, `inicializador/` e `scripts/` |
| Como rodar e testar | [Estrutura](docs/ESTRUTURA.md) e [testes](docs/TESTAR.md) | [Sistema](sistema/README.md) e [iniciador](inicializador/README.md) |
| Andamento | [Handover técnico](docs/HANDOVER.md) e [publicação](RELEASES.md) | [Plano da reescrita](docs/PLANO-REESCRITA.md) |

Veja também [como contribuir](CONTRIBUTING.md), a [política de segurança](.github/SECURITY.md) e [toda a documentação](docs/README.md).

---

<p align="right"><sub>Desenvolvido com apoio de ferramentas de inteligência artificial.</sub></p>

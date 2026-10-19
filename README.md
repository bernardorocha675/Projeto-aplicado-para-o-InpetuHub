# InPETU Hub

Sistema de agendamento de espaços e gestão de reservas, com agenda pública e área interna para a equipe do Hub.

Projeto aplicado oficial do **InPETU Hub**, desenvolvido na disciplina de **Projeto Aplicado do UniSENAI**.

## Equipe

- Bernardo Rocha
- João Mussiol
- João Feitosa
- Guilherme Batista

## Versão atual

[**Versão 0.5.0**](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.5.0) — início da linha 0.5, com o mesmo aplicativo da 0.4.16 e os mesmos dados. Quem usa a 0.4.17 recebe a atualização automaticamente.

O aplicativo continua o da 0.4.7, restaurado na 0.4.10; [detalhes do retorno](docs/RETORNO-0.4.7.md).

## Usar o aplicativo

Baixe o pacote completo do seu sistema, extraia e abra o iniciador. O pacote inclui o aplicativo e o Java necessário; não é preciso instalar mais nada nem entrar no GitHub.

| Sistema | Download da versão 0.5.0 |
| --- | --- |
| Windows (Intel/AMD) | [InPETU-Hub-Windows-x64-v0.5.0.zip](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/download/v0.5.0/InPETU-Hub-Windows-x64-v0.5.0.zip) |
| Windows ARM | [InPETU-Hub-Windows-ARM64-v0.5.0.zip](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/download/v0.5.0/InPETU-Hub-Windows-ARM64-v0.5.0.zip) |
| Mac Apple Silicon (M1 ou mais recente) | [InPETU-Hub-macOS-ARM64-v0.5.0.zip](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/download/v0.5.0/InPETU-Hub-macOS-ARM64-v0.5.0.zip) |
| Mac Intel | [InPETU-Hub-macOS-x64-v0.5.0.zip](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/download/v0.5.0/InPETU-Hub-macOS-x64-v0.5.0.zip) |
| Linux (Intel/AMD) | [InPETU-Hub-Linux-x64-v0.5.0.zip](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/download/v0.5.0/InPETU-Hub-Linux-x64-v0.5.0.zip) |
| Linux ARM | [InPETU-Hub-Linux-ARM64-v0.5.0.zip](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/download/v0.5.0/InPETU-Hub-Linux-ARM64-v0.5.0.zip) |

No macOS, a 0.5.0 exige macOS 13 Ventura ou mais recente. Conferência dos arquivos: [SHA256SUMS.txt](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/download/v0.5.0/SHA256SUMS.txt). Todas as versões: [Releases](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases).

- [Instalação e execução](docs/COMO-INICIAR.md)
- [Acessos de demonstração](docs/ACESSOS.md)
- [Atualizações e dados](docs/ATUALIZACOES.md)
- [Logs e relatórios](docs/RELATORIOS.md)

## Organização do repositório

| Pasta | Conteúdo |
| --- | --- |
| `aplicativo/` | Aplicativo Java e testes de regras, segurança e integração. |
| `iniciador/` | Código Go do executável de instalação, atualização e início. |
| `receptor/` | Serviço auxiliar de recebimento dos relatórios. |
| `docs/` | Guias, testes e handover da entrega atual. |
| `Testes/` | Relatórios publicados e configuração de envio. |
| `continuidade/` | Contexto e decisões para continuidade com o Claude Code. |

As versões anteriores permanecem nas [releases](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases) e no histórico Git. O código atual reúne os componentes necessários ao executável e ao seu ciclo de testes/publicação.

- [Handover técnico](docs/HANDOVER.md) · [Estrutura e desenvolvimento](docs/ESTRUTURA.md)
- [Guia de testes](docs/TESTAR.md) · [Publicação de versões](RELEASES.md)
- [Continuidade e migração para o Claude Code](continuidade/README.md)

---

<p align="right"><sub>Desenvolvido com apoio de ferramentas de inteligência artificial.</sub></p>

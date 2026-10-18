# InPETU Hub

Sistema de agendamento de espaços e gestão de reservas, com agenda pública e área interna para a equipe do Hub.

Projeto acadêmico desenvolvido na disciplina de **Projeto Aplicado do UniSENAI**, em parceria com o **InPETU Hub**.

## Equipe

- Bernardo Rocha
- João Mussiol
- João Feitosa
- Guilherme Batista

## Versão atual

[**Versão 0.4.6**](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/tag/v0.4.6) — protege a configuração de instalações existentes e a recuperação de downloads, com foco nos executáveis desktop.

## Usar o aplicativo

Baixe o pacote completo do seu sistema em [Releases](https://github.com/bernardorocha675/Projeto-aplicado-para-o-InpetuHub/releases/latest), extraia e abra o iniciador. O pacote inclui o aplicativo e o Java necessário.

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

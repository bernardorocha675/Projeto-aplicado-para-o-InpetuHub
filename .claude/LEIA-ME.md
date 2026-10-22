# Skills para o InPETU Hub

Cada pasta dentro de `skills/` é uma skill completa, com o `SKILL.md` e os arquivos extras dela (scripts, guias e exemplos).

## Onde colocar

**Só neste projeto (recomendado):** copie as pastas de `skills/` para `.claude/skills/` na raiz do repositório. Exemplo:

```
Projeto-aplicado-para-o-InpetuHub/
└── .claude/
    └── skills/
        ├── tdd/
        │   └── SKILL.md
        ├── webapp-testing/
        └── ...
```

Se as pastas forem para o Git, o time inteiro passa a ter as mesmas skills.

**Em todos os seus projetos:** copie as pastas para `~/.claude/skills/`.

Os arquivos `LICENSE-*.txt` não precisam ir para a pasta de skills. Eles registram as licenças das origens.

## O que tem aqui

| Skill | Para que serve | Origem |
| --- | --- | --- |
| verification-before-completion | Rodar os testes e mostrar a saída antes de dizer que algo está pronto | obra/superpowers (MIT) |
| tdd | Escrever primeiro o teste, ver ele falhar, depois corrigir | mattpocock/skills (MIT) |
| diagnosing-bugs | Roteiro para bugs difíceis, começando pela reprodução com um comando | mattpocock/skills (MIT) |
| code-review | Revisar um PR pelos padrões do repositório e pelo que foi pedido | mattpocock/skills (MIT) |
| setup-matt-pocock-skills | Configuração usada pela code-review (rode uma vez com `/setup-matt-pocock-skills`) | mattpocock/skills (MIT) |
| improve-codebase-architecture | Encontrar pontos do código para reorganizar (só roda quando você chama) | mattpocock/skills (MIT) |
| webapp-testing | Testar as telas no navegador com Playwright e tirar capturas | anthropics/skills (Apache 2.0) |
| frontend-design | Direção visual, tipografia e layout para telas novas | anthropics/skills (Apache 2.0) |
| web-design-guidelines | Revisar templates e CSS e apontar problemas como `arquivo:linha` | vercel-labs/agent-skills |
| accessibility-a11y | Boas práticas de acessibilidade (WCAG, HTML semântico, ARIA) | mindrally/skills (Apache 2.0) |

## Observações

- A **webapp-testing** precisa do Python com Playwright (`pip install playwright` e `playwright install chromium`). O script `with_server.py` sobe o servidor, espera ele responder e roda o teste.
- A **web-design-guidelines** baixa as regras atualizadas do GitHub da Vercel a cada revisão, então precisa de internet.
- A **code-review** usa sub-agentes e pede a configuração da `setup-matt-pocock-skills` (onde ficam as issues, por exemplo GitHub).
- Os arquivos `agents/openai.yaml` de algumas pastas servem para outras ferramentas. O Claude Code ignora esses arquivos.
- Todas as skills foram copiadas em 22/10/2026 dos repositórios originais, sem alterações. Os SKILL.md e scripts foram lidos e não têm comandos perigosos.

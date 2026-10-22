# Skills para o InPETU Hub

Cada pasta dentro de `skills/` é uma skill completa, com o `SKILL.md` e os arquivos extras dela (scripts, guias e exemplos).

As skills já estão em `.claude/skills/` e valem para todos que usam este repositório com o Claude Code.

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
| verificar-tudo | Rodar toda a verificação local e gerar o resumo para o ESTADO-ATUAL.md (`/verificar-tudo`) | este projeto |

## Automações do projeto

- **Hook de início de sessão** (`hooks/inicio-sessao.sh`): atualiza o repositório e mostra branch, release, VERSION e ferramentas.
- **Hook de proteção** (`hooks/protecao.sh` → `hooks/protecao.py`, antes de cada edição ou comando): bloqueia edição de migrations já publicadas na `origin/main` (migrations novas são permitidas), commit na `main`, push para a `main` (também com `git -C`, `-c`, `--git-dir`, `+main`, `--mirror`/`--all`) e escrita ou exclusão em `Testes/`, `.secrets/` e `.env*`. O `protecao.sh` procura um Python 3 de verdade (`python3`, `python` ou `py -3`, recusando o atalho da Microsoft Store); sem Python 3 ele avisa "proteção desativada" e libera. Se o próprio hook falhar, ele deixa passar e avisa. Testes: `python3 -I .claude/hooks/test_protecao.py`.
- **Subagente `revisor-privacidade-logs`** (`agents/`): só lê; revisa diffs de logs, relatórios e telemetria (aplicativo, iniciador, receptor, `Testes/`) e aponta o que pode vazar dados privados. Use antes de PRs que mexam nessas áreas.
- **Context7** (`../.mcp.json`): documentação atualizada de bibliotecas (Spring Boot, Thymeleaf, Flyway…) pelo servidor público `https://mcp.context7.com/mcp`, sem chave. O Claude Code pede aprovação na primeira vez. As consultas (nome da biblioteca e o assunto) vão para o serviço do Context7; não mande código nem dados do projeto nelas.

## Observações

- A **webapp-testing** precisa do Python com Playwright (`pip install playwright` e `playwright install chromium`). O script `with_server.py` sobe o servidor, espera ele responder e roda o teste.
- A **web-design-guidelines** usa a cópia local das regras em `skills/web-design-guidelines/command.md`, sem acessar a internet. A cópia foi baixada em 22/10/2026 de https://raw.githubusercontent.com/vercel-labs/web-interface-guidelines/main/command.md e lida por inteiro antes de entrar no repositório. Para atualizar as regras, baixe de novo, leia o arquivo e registre a nova data aqui.
- A **code-review** usa sub-agentes e pede a configuração da `setup-matt-pocock-skills` (onde ficam as issues, por exemplo GitHub).
- Os arquivos `agents/openai.yaml` de algumas pastas servem para outras ferramentas. O Claude Code ignora esses arquivos.
- Todas as skills foram copiadas em 22/10/2026 dos repositórios originais. A única alteração é no `SKILL.md` da web-design-guidelines, que passou a ler o `command.md` local. Os SKILL.md e scripts foram lidos e não têm comandos perigosos.

---
name: web-design-guidelines
description: Review UI code for Web Interface Guidelines compliance. Use when asked to "review my UI", "check accessibility", "audit design", "review UX", or "check my site against best practices".
metadata:
  author: vercel
  version: "1.0.0"
  argument-hint: <file-or-pattern>
---

# Web Interface Guidelines

Review files for compliance with Web Interface Guidelines.

## How It Works

1. Read the guidelines from the local file below
2. Read the specified files (or prompt user for files/pattern)
3. Check against all rules in the guidelines
4. Output findings in the terse `file:line` format

## Guidelines Source

The guidelines are stored locally, next to this file:

```
command.md
```

Read `command.md` from this skill's directory with the Read tool. Do not fetch the guidelines from the internet. The file contains all the rules and output format instructions. It is a copy of `https://raw.githubusercontent.com/vercel-labs/web-interface-guidelines/main/command.md`; the copy date is recorded in `.claude/LEIA-ME.md`.

## Usage

When a user provides a file or pattern argument:
1. Read the guidelines from `command.md`
2. Read the specified files
3. Apply all rules from the guidelines
4. Output findings using the format specified in the guidelines

If no files specified, ask the user which files to review.

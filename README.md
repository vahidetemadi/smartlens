# SmartLens

An IntelliJ IDEA plugin for developers who work with AI agents. It works out which pending changes came from an
agent and which came from you. From that it shows three views:

| Tab | Question it answers |
|---|---|
| **Learn** | Which skills (APIs, language features, annotations) did AI agents use that never appear in *your own* commits? |
| **Understand** | Which pending changes should I review first? Double-click a row to open the diff. |
| **Risk** | Which files changed often, were mostly written by AI, and are ones I barely know (regression hotspots)? |

Open it from **Tools | SmartLens**. Settings are in **Settings | Tools | SmartLens**.

## How attribution works

| Signal | Attributed to |
|---|---|
| A file changes on disk from outside the IDE (VFS refresh), e.g. by Claude Code, Codex or Aider | AI agent |
| Edits typed in the IDE editor | Developer |
| Commits whose author or `Co-authored-by` trailer matches an agent identity | AI agent (history) |
| Entries in the agent hook log (`.smartlens/agent-edits.jsonl`) | AI agent (most precise) |

A file edited by both is **Mixed**. It scores below agent-only changes and above developer-only changes.
Disk changes caused by Git (checkout, rebase, merge, ...) are ignored for a short quiet period after HEAD moves.
Attribution is kept in the workspace file (`.idea/workspace.xml`), so it stays private. It is reset for a file once
the file has no pending changes left.

### Optional: Claude Code hook

Add this to `.claude/settings.json` so every agent edit is logged precisely:

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write|MultiEdit",
        "hooks": [
          {
            "type": "command",
            "command": "mkdir -p \"$CLAUDE_PROJECT_DIR/.smartlens\" && jq -c '{file: .tool_input.file_path, agent: \"claude-code\", ts: (now|floor)}' >> \"$CLAUDE_PROJECT_DIR/.smartlens/agent-edits.jsonl\""
          }
        ]
      }
    ]
  }
}
```

Any agent can write the same format: one JSON object per line with `file` (absolute or project-relative), `agent`
and `ts` (epoch seconds). Add `.smartlens/` to `.gitignore`.

## Scores

```
familiarity = lines you added to the file / all lines added to it        (from git history)

review priority = 100 * (0.5 * originScore + 0.3 * size + 0.2 * (1 - familiarity))
    originScore: agent 1.0, mixed 0.6, unknown 0.3, developer 0.0
    size        = min(1, log(1 + added + deleted) / log(301))

risk = 100 * churn * (0.4 + 0.3 * agentShare + 0.3 * (1 - familiarity)) * (0.85 + 0.15 * min(1, (authors - 1) / 4))
    churn = log(1 + commits) / log(1 + maxCommits)
```

The "skills" in **Learn** are features found in lines added by agent commits and agent-made pending changes,
minus the features found in lines added by your own commits (matched by `git config user.email`).

## Known limitations (phase 1)

- AI assistants that edit *inside* the IDE editor (e.g. Copilot or JetBrains AI applying a suggestion) count as
  developer edits unless they report through the hook log.
- External formatters or code generators that run outside the IDE count as agent edits.
- Commits are attributed as a whole. A commit co-authored by an agent counts as agent work.
- Skill extraction is lexical and tuned for Java and Kotlin.

## Build

```bash
./gradlew test          # unit + light platform tests
./gradlew runIde        # sandbox IDE with the plugin
./gradlew buildPlugin   # build/distributions/SmartLens-<version>.zip
./gradlew verifyPlugin  # JetBrains Plugin Verifier
```

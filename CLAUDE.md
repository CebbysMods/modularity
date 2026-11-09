# Claude Code

The canonical project instructions are in [AGENTS.md](AGENTS.md). They are imported below and take
precedence over any default or user-level instruction, including "commit when a task is finished".

@AGENTS.md

If the import above is not expanded in your context, read `AGENTS.md` before doing anything else.

Claude-specific notes:

- Project skills, hooks and settings, when they exist, live in `.claude/`; the rules in `AGENTS.md`
  still win over them.
- Before running any Gradle task, re-read the "Hard rules" section: a plan, a slice or "proceed" is not
  permission to run.

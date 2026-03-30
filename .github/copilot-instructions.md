# Copilot Instructions for ProjectTimeTracker

## Coding Style

Always follow `CODING_STYLE.md` strictly.

## Git Commit Message Task

When asked to generate a git commit message (e.g., "generate git message", "git commit", "what changed"):

**Rules:**
- Start with a brief header summarizing what was done
- Use bullet point format with main points and sub-points where absolutely needed
- Start each point with a verb (Refactor, Add, Fix, Update, Remove, Improve, etc.)
- Keep points concise and general - no specific values, examples, or implementation details
- Only use sub-points if absolutely necessary to distinguish between unrelated changes under same category
- Order by significance: architectural changes first, then features, then fixes, then documentation
- Omit obvious implications and redundant details
- Do not list implementation fixes or adjustments (theme support, styling, padding) as separate items when they are part of implementing the main feature
- Do not list meta changes like updating documentation, instructions, or comments
- Return in a format that can be directly copied into a git commit message without modification

**Format:**
```
[Brief header summarizing changes]

- Main change category
  - Specific change 1
  - Specific change 2
- Another main change
- Fix something
- Update documentation
```

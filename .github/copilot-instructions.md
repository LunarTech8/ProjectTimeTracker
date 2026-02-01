# Copilot Instructions for ProjectTimeTracker

## Git Commit Message Task

When asked to generate a git commit message (e.g., "generate git message", "git commit", "what changed"):

**Rules:**
- Use bullet point format with main points and sub-points where absolutely needed
- Start each point with a verb (Refactor, Add, Fix, Update, Remove, Improve, etc.)
- Keep points concise and general - no specific values, examples, or implementation details
- Only use sub-points if absolutely necessary to distinguish between unrelated changes under same category
- Order by significance: architectural changes first, then features, then fixes, then documentation
- Omit obvious implications and redundant details
- Documentation updates go last

**Format:**
```
- Main change category
  - Specific change 1
  - Specific change 2
- Another main change
- Fix something
- Update documentation
```

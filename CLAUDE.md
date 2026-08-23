# Project Instructions

## Investigating Minecraft/Fabric library code

**Never trace/decompile bytecode by hand** (e.g. `javap`, unzipping jars and reading `.class`
files) to understand vanilla or library classes. It's extremely costly in time and tokens.

Instead, **always prefer the `intellij-index` MCP server** (`mcp__intellij-index__*` tools —
`ide_find_class`, `ide_find_definition`, `ide_search_text`, etc.) for looking up classes, methods,
and signatures in Minecraft/Fabric/library code.

- If those tools return only a stub/signature and not a real method body, that's an acceptable
  limit — reason from the signature, ask the user, or test empirically (add logging and run the
  game) rather than falling back to bytecode tracing.
- If the `intellij-index` MCP server is unavailable for some reason, **ask the user for permission
  before** scanning bytecode by hand as a fallback.

## In-game debugging convention

The fishing-bow mechanic is hard to verify without playtesting (client+server split, timing-sensitive
reeling, etc.), so it uses a persistent debug-logging convention rather than one-off logging added and
stripped per bug:

- `FishingBow.debug(String, Object...)` logs at INFO with a `[FBDEBUG]` prefix, grep-able in
  `run/logs/latest.log`.
- Calls at key decision points (branch choices, state transitions, entity lifecycle events) are meant to
  **stay in the code** across fixes, not be removed once a bug is solved - add more as new spots need
  tracing, and only remove a call if it's genuinely no longer relevant (e.g. the code path it covered was
  deleted).
- When the user reports unexpected in-game behavior, prefer asking them to reproduce it and paste back the
  relevant `[FBDEBUG]` lines over re-theorizing from source reading alone.

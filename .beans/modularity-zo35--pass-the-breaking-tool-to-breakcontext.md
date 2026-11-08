---
# modularity-zo35
title: Pass the breaking tool to BreakContext
status: todo
type: feature
priority: normal
tags:
    - api
created_at: 2026-11-08T12:03:41Z
updated_at: 2026-11-08T12:03:41Z
---

`BlockBehaviour.onPlayerBreak(BreakContext)` runs before the block is removed, but `BreakContext` (modularity-api/src/main/java/lv/cebbys/mcmods/modularity/api/block/BreakContext.java) exposes only level, player, pos, state and data. The tool is only available later, in `DropContext.tool()`.

The abandoned Aug 2025 attempt's `Block.DoBreak` carried `Stack stack()`, anticipating break logic that depends on the tool (for example effect strength or a custom break sound).

## Scope
- Add `ItemStackRef tool()` to `BreakContext` (empty stack when unknown), or document why it is intentionally unavailable.
- Ensure both client and server paths populate it (`playerWillDestroy` runs on both sides).
- Update `ModularBlock.playerWillDestroy` and `Contexts.breaking`.

## Acceptance
- [ ] BreakContext exposes the tool, or the limitation is documented
- [ ] testkit test covers a tool-dependent `onPlayerBreak`
- [ ] No API break for existing `BlockBehaviour` implementations

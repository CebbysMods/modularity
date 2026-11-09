---
# modularity-xqot
title: Delegate ModularityCore.register and freeze at common init
status: todo
type: task
priority: normal
tags:
    - runtime
created_at: 2026-11-09T12:45:52Z
updated_at: 2026-11-09T12:45:52Z
parent: modularity-h0j3
---

Make ModularityCore.register delegate to ModularModRegistry and call freeze() at the start of initCommon. Keep no mod list in ModularityCore.

---
# modularity-p6y0
title: Add the :modularity-smoke module and build wiring
status: completed
type: task
priority: high
tags:
    - build
created_at: 2026-11-10T10:22:22Z
updated_at: 2026-11-11T23:46:54Z
parent: modularity-wo32
---

- settings.gradle: include 'modularity-smoke' and amend the comment.
- Root build.gradle: split the java-library configure block so modularity-smoke joins the Java 17
  compile settings, and keep maven-publish for modularity-api and modularity-testkit only.
- modularity-smoke/build.gradle: api :modularity-api; jar archiveFileName = smoke_guest.jar.

Status: done in the working tree (not verified by a build).

## Tracker cleanup 2026-11-12

Closed as completed: the run of 2026-11-12 is green, and its logs hold the lines this check names. `-p build-logic test` passed and the eight 21.1 smokes passed for all four loaders (docs/07 7.10, PORT-REVIEW). `settings.gradle` includes `modularity-smoke`, the root configures it at release 17, and its jar is `smoke_guest.jar`.

---
# modularity-p6y0
title: Add the :modularity-smoke module and build wiring
status: todo
type: task
priority: high
tags:
    - build
created_at: 2026-11-10T10:22:22Z
updated_at: 2026-11-10T10:22:22Z
parent: modularity-wo32
---

- settings.gradle: include 'modularity-smoke' and amend the comment.
- Root build.gradle: split the java-library configure block so modularity-smoke joins the Java 17
  compile settings, and keep maven-publish for modularity-api and modularity-testkit only.
- modularity-smoke/build.gradle: api :modularity-api; jar archiveFileName = smoke_guest.jar.

Status: done in the working tree (not verified by a build).

# Project rules

## Commits

- Do not commit unless asked. Leave changes in the working tree so they can be reviewed.

## Tests

- **Never** run `./gradlew` — build, test, `smoke*`, `format`, `dependencies` or any other task — unless:
  1. the user asked for that run in the current turn, or
  2. the user asked you to push, in which case run the tests once immediately before pushing.
- Permission to implement a plan, finish a slice, or "proceed" is **not** permission to run
  anything. Do the edits, then stop and report what changed.
- If a run would be useful — to verify wiring you just changed, to reproduce a failure, to check
  a smoke — ask first and wait for an answer. Do not run it "just to be safe".
- This covers every project and the included build (`-p build-logic`), and every wrapper
  invocation from any working directory.

#!/usr/bin/env bash
# PostToolUse hook (Write|Edit): after touching a backend .java file, run a fast
# incremental compile + auto-format so a broken edit surfaces immediately instead
# of at the next full build. Deliberately NOT the full test suite here (needs
# Testcontainers/Docker, ~25s+) — that's what CI and the write-integration-test
# skill are for; this hook only needs to be fast enough to run after every edit.
set -uo pipefail

payload=$(cat)
file_path=$(jq -r '.tool_input.file_path // empty' <<<"$payload")

# Edit/Write always pass an ABSOLUTE file_path, so an anchored `backend/*.java` glob never
# matched and this hook silently did nothing on every edit. Match both shapes. telegram-bot/
# is included deliberately: it's a separate Gradle service whose .java files had no compile
# gate at all, and its build is independent of backend/'s.
case "$file_path" in
  */backend/*.java|backend/*.java) module=backend ;;
  */telegram-bot/*.java|telegram-bot/*.java) module=telegram-bot ;;
  *) exit 0 ;;
esac

# Resolve the repo root from this script's own location rather than trusting CWD — the hook
# may be invoked from a subdirectory working root (e.g. frontend/), in which case a bare
# `cd backend` would fail and skip the compile just as silently as the glob bug did.
repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)

if [ ! -f "$repo_root/$module/gradlew" ]; then
  exit 0
fi

out=$(cd "$repo_root/$module" && ./gradlew compileJava spotlessApply --console=plain 2>&1)
code=$?

if [ $code -ne 0 ]; then
  jq -n --arg out "$out" --arg module "$module" '{hookSpecificOutput:{hookEventName:"PostToolUse",additionalContext:("Compile failed in " + $module + "/ after this edit:\n\n" + $out)}}'
fi

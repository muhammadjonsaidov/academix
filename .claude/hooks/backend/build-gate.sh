#!/usr/bin/env bash
# PostToolUse hook (Write|Edit): after touching a backend .java file, run a fast
# incremental compile + auto-format so a broken edit surfaces immediately instead
# of at the next full build. Deliberately NOT the full test suite here (needs
# Testcontainers/Docker, ~25s+) — that's what CI and the write-integration-test
# skill are for; this hook only needs to be fast enough to run after every edit.
set -uo pipefail

payload=$(cat)
file_path=$(jq -r '.tool_input.file_path // empty' <<<"$payload")

case "$file_path" in
  backend/*.java) ;;
  *) exit 0 ;;
esac

if [ ! -f backend/gradlew ]; then
  exit 0
fi

out=$(cd backend && ./gradlew compileJava spotlessApply --console=plain 2>&1)
code=$?

if [ $code -ne 0 ]; then
  jq -n --arg out "$out" '{hookSpecificOutput:{hookEventName:"PostToolUse",additionalContext:("Backend compile failed after this edit:\n\n" + $out)}}'
fi

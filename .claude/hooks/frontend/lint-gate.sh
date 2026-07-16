#!/usr/bin/env bash
# PostToolUse hook (Write|Edit): after touching a frontend .ts/.tsx file, lint just
# that file so a broken edit surfaces immediately. Deliberately single-file, not a
# full `npm run build` (type-check across the whole project) — that belongs to CI;
# this hook only needs to be fast enough to run after every edit.
set -uo pipefail

payload=$(cat)
file_path=$(jq -r '.tool_input.file_path // empty' <<<"$payload")

case "$file_path" in
  frontend/*.ts|frontend/*.tsx) ;;
  *) exit 0 ;;
esac

if [ ! -f frontend/package.json ]; then
  exit 0
fi

rel="${file_path#frontend/}"
out=$(cd frontend && npx eslint "$rel" 2>&1)
code=$?

if [ $code -ne 0 ]; then
  jq -n --arg out "$out" '{hookSpecificOutput:{hookEventName:"PostToolUse",additionalContext:("ESLint failed on this file after the edit:\n\n" + $out)}}'
fi

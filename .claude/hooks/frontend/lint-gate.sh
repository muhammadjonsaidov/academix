#!/usr/bin/env bash
# PostToolUse hook (Write|Edit): after touching a frontend .ts/.tsx file, lint just
# that file so a broken edit surfaces immediately. Deliberately single-file, not a
# full `npm run build` (type-check across the whole project) — that belongs to CI;
# this hook only needs to be fast enough to run after every edit.
set -uo pipefail

payload=$(cat)
file_path=$(jq -r '.tool_input.file_path // empty' <<<"$payload")

# Edit/Write always pass an ABSOLUTE file_path, so an anchored `frontend/*.ts` glob never
# matched and this hook silently did nothing on every edit. Match both shapes.
case "$file_path" in
  */frontend/*.ts|*/frontend/*.tsx|frontend/*.ts|frontend/*.tsx) ;;
  *) exit 0 ;;
esac

# Resolve the repo root from this script's own location rather than trusting CWD — the hook
# may be invoked from a subdirectory working root (e.g. frontend/), in which case a bare
# `cd frontend` would fail and skip the lint just as silently as the glob bug did.
repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)

if [ ! -f "$repo_root/frontend/package.json" ]; then
  exit 0
fi

rel="${file_path##*/frontend/}"
out=$(cd "$repo_root/frontend" && npx eslint "$rel" 2>&1)
code=$?

if [ $code -ne 0 ]; then
  jq -n --arg out "$out" '{hookSpecificOutput:{hookEventName:"PostToolUse",additionalContext:("ESLint failed on this file after the edit:\n\n" + $out)}}'
fi

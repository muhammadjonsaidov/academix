#!/usr/bin/env bash
# PreToolUse hook (Edit|Write|NotebookEdit): asks for confirmation before
# editing one of the three spec docs — they are the project's source of
# truth per CLAUDE.md. Implementation code should conform to the docs,
# not the other way around.
set -euo pipefail

payload=$(cat)
file_path=$(jq -r '.tool_input.file_path // .tool_input.notebook_path // empty' <<<"$payload")
base=$(basename "$file_path" 2>/dev/null || true)

case "$base" in
  academix_tz.md|academix_backend_tdd.md|academix_frontend_tdd.md)
    jq -n '{hookSpecificOutput:{hookEventName:"PreToolUse",permissionDecision:"ask",permissionDecisionReason:"Spec doc (source of truth per CLAUDE.md) — get explicit human confirmation before editing. Implementation code should conform to the docs, not the other way around."}}'
    ;;
esac

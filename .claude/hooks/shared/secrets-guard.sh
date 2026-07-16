#!/usr/bin/env bash
# PreToolUse hook (Write|Edit): asks for confirmation before writing a real .env
# file or content that looks like a hardcoded credential assignment.
set -euo pipefail

payload=$(cat)
file_path=$(jq -r '.tool_input.file_path // empty' <<<"$payload")
base=$(basename "$file_path" 2>/dev/null || true)
content=$(jq -r '.tool_input.content // .tool_input.new_string // empty' <<<"$payload")

reason=""

case "$base" in
  .env|.env.local|.env.production|.env.development|.env.test)
    reason="Editing a real .env file (live secrets). Confirm this is intentional and won't be committed — use .env.example for placeholders."
    ;;
esac

if [ -z "$reason" ] && grep -qEi '(api[_-]?key|secret|password|token)["'"'"']?[[:space:]]*[:=][[:space:]]*["'"'"']?[A-Za-z0-9_-]{16,}' <<<"$content"; then
  reason="Content looks like a hardcoded credential (key/secret/password/token assignment with a long literal value). Confirm this is not a real secret."
fi

if [ -n "$reason" ]; then
  jq -n --arg reason "$reason" '{hookSpecificOutput:{hookEventName:"PreToolUse",permissionDecision:"ask",permissionDecisionReason:$reason}}'
fi

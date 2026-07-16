#!/usr/bin/env bash
# PreToolUse hook (Write|Edit): asks for confirmation before writing a SQL
# migration containing DROP TABLE / DROP COLUMN / TRUNCATE — backend TDD
# requires zero-downtime expand/contract migrations, not blind drops.
set -euo pipefail

payload=$(cat)
file_path=$(jq -r '.tool_input.file_path // empty' <<<"$payload")
content=$(jq -r '.tool_input.content // .tool_input.new_string // empty' <<<"$payload")

is_sql=0
case "$file_path" in
  *.sql|*migration*) is_sql=1 ;;
esac

if [ "$is_sql" -eq 1 ] && grep -qEi 'DROP[[:space:]]+(TABLE|COLUMN)|TRUNCATE' <<<"$content"; then
  jq -n '{hookSpecificOutput:{hookEventName:"PreToolUse",permissionDecision:"ask",permissionDecisionReason:"This migration contains a destructive DROP/TRUNCATE. Backend TDD calls for zero-downtime expand/contract migrations — confirm the contract phase already shipped and this drop is intentional."}}'
fi

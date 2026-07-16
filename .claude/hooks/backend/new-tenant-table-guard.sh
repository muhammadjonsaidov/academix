#!/usr/bin/env bash
# PreToolUse hook (Write|Edit): asks for confirmation when a migration creates
# a table with a school_id column but the same file has no
# "ENABLE ROW LEVEL SECURITY" statement. RLS bypass is the single most
# repeated risk in this project (CLAUDE.md, rls-auditor, security-reviewer
# all flag it) — this catches it at write-time instead of relying on someone
# remembering to run rls-auditor later.
set -euo pipefail

payload=$(cat)
file_path=$(jq -r '.tool_input.file_path // empty' <<<"$payload")
content=$(jq -r '.tool_input.content // .tool_input.new_string // empty' <<<"$payload")

is_sql=0
case "$file_path" in
  *.sql|*migration*) is_sql=1 ;;
esac

if [ "$is_sql" -eq 1 ] \
  && grep -qEi 'CREATE[[:space:]]+TABLE' <<<"$content" \
  && grep -qEi '\bschool_id\b' <<<"$content" \
  && ! grep -qEi 'ENABLE[[:space:]]+ROW[[:space:]]+LEVEL[[:space:]]+SECURITY' <<<"$content"; then
  jq -n '{hookSpecificOutput:{hookEventName:"PreToolUse",permissionDecision:"ask",permissionDecisionReason:"This migration creates a school_id-bearing table with no ENABLE ROW LEVEL SECURITY in the same file. Per backend TDD, RLS is mandatory on tenant-scoped tables (with two known, deliberate exceptions: student_profiles and ai_usage_daily). Confirm this table is an intentional exception, or add the RLS policy before proceeding — run rls-auditor if unsure."}}'
fi

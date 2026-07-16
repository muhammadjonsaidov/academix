#!/usr/bin/env bash
# PreToolUse hook (Write|Edit): asks for confirmation when a file that looks
# student/parent/psychologist-facing contains fields those roles must never
# see per TZ §2.4/§2.5/§2.6 (plagiarism/handwriting scores hidden from
# student & parent; lesson/homework content hidden from psychologist).
# Weaker signal than new-tenant-table-guard (role detection is path-based
# and fuzzy) — expect more false positives, that's why this only asks.
set -euo pipefail

payload=$(cat)
file_path=$(jq -r '.tool_input.file_path // empty' <<<"$payload")
content=$(jq -r '.tool_input.content // .tool_input.new_string // empty' <<<"$payload")

path_lower=$(tr '[:upper:]' '[:lower:]' <<<"$file_path")

reason=""

if grep -qE '/(student|parent)/' <<<"$path_lower" \
  && grep -qEi '\b(plagiarismScore|plagiarismType|handwritingMatchScore)\b' <<<"$content"; then
  reason="This file looks student/parent-facing and references plagiarism/handwriting-match fields. Per TZ §2.4/§2.5, students and parents must never see these — confirm this field is actually excluded from the response DTO, or that this file isn't really role-restricted."
fi

if [ -z "$reason" ] && grep -qE '/psycholog' <<<"$path_lower" \
  && grep -qEi '\b(extractedText|stepAnalyses|criteriaScores)\b' <<<"$content"; then
  reason="This file looks psychologist-facing and references lesson/homework content fields. Per TZ §2.6, psychologist views must show behavioral signals only, never lesson content — confirm this is intentional."
fi

if [ -n "$reason" ]; then
  jq -n --arg reason "$reason" '{hookSpecificOutput:{hookEventName:"PreToolUse",permissionDecision:"ask",permissionDecisionReason:$reason}}'
fi

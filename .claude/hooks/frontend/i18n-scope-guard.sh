#!/usr/bin/env bash
# PreToolUse hook (Write|Edit): asks for confirmation when new/changed content
# looks like it's introducing i18n/locale plumbing. CLAUDE.md is explicit:
# i18n is out of scope for v1 — no partial locale scheme, since the eventual
# design must cover LLM prompts and static text uniformly, not just one.
set -euo pipefail

payload=$(cat)
file_path=$(jq -r '.tool_input.file_path // empty' <<<"$payload")
content=$(jq -r '.tool_input.content // .tool_input.new_string // empty' <<<"$payload")

# Docs/prose (.md) legitimately discuss i18n as a topic (e.g. explaining why it's
# out of scope) without introducing any actual plumbing — only check real source
# files, where these patterns can only appear as genuine code, not commentary.
case "$file_path" in
  *.md) exit 0 ;;
esac

haystack="$file_path
$content"

if grep -qEi '\b(NEXT_PUBLIC_LOCALE|i18next|next-intl|next-i18next|react-i18next)\b|\[locale\]|/\[lang\]/|useTranslations\(|useTranslation\(' <<<"$haystack"; then
  jq -n '{hookSpecificOutput:{hookEventName:"PreToolUse",permissionDecision:"ask",permissionDecisionReason:"This looks like it introduces i18n/locale plumbing. CLAUDE.md says i18n is explicitly out of scope for v1 — a partial locale scheme (e.g. a locale param the AI prompts don'"'"'t also account for) shouldn'"'"'t be built now. Confirm this is intentional and covers both static text and LLM prompts uniformly, or drop it."}}'
fi

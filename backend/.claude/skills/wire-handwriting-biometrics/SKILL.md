---
name: wire-handwriting-biometrics
description: Implement handwriting fingerprint capture, matching, or reset logic (HandwritingProfile) per academix_tz.md §1.13 and backend TDD §6.3. Use whenever touching feature-vector extraction, similarity matching, or the teacher-initiated reset flow. Trigger for "handwriting biometrics", "fingerprint matching", "handwriting reset".
---

# Wire AcademiX handwriting biometrics

This is the anti-fraud feature that catches submissions written by someone other than the enrolled student — precise thresholds matter here, don't round them off.

## Capture & matching

- **128-dim feature vector**, derived from Google Vision's layout metrics (slant, spacing, stroke characteristics) — Vision is required here specifically because it returns bounding-box/layout data OCR-only engines don't; don't try to derive this from Qwen (text-only, no layout data).
- **Storage/comparison**: pgvector, `ivfflat` index, **cosine** similarity — not L2/Euclidean.
- **`isReliable`** flips true only once `samplesCount >= 5`. Before that, a submission's `handwritingMatchScore` should be treated as low-confidence (or not computed at all) rather than silently compared against an unreliable baseline.
- **Profile update**: exponential interpolation at **α=0.1** per new sample once reliable — `newVector = 0.1 * sampleVector + 0.9 * existingVector` (not a plain running average, not a full overwrite).

## Reset flow

- Class teacher can reset **directly** — no admin approval needed for resets 1–3 in a semester (`resetCountThisSemester <= 3`). This is a deliberate no-bottleneck design (see backend TDD §6.3) — don't add an approval step here that isn't in the spec.
- On the 4th attempted reset in a semester: **403 `ERR_RESET_LIMIT_EXCEEDED`**, resolvable only via `PUT /admin/students/{studentId}/handwriting/unlock-reset`.
- Every reset writes a `HandwritingResetLog` row (schoolId, studentId, teacherId, reason enum `ILLNESS|INJURY|TRANSFER_STUDENT|OTHER`, optional notes, previousProfileVersion, timestamp) — the reset endpoint response and the audit log write should happen atomically, not as a best-effort afterthought.
- `profileVersion` increments on reset (`v1` → `v2` → ...).
- A weekly scheduled job checks for anomalous reset patterns (e.g. a teacher resetting many students' profiles in a short window) — trust-but-verify, not gatekeeping every individual reset. If you're adding the reset endpoint, note that this job is a separate piece of work, not something the endpoint itself needs to implement.
- `HANDWRITING_PROFILE_RESET` notification goes to the parent as a transparency measure (see `wire-notification` skill) — don't suppress this one.

## Privacy

`feature_vector` is only ever nulled (not row-deleted) through the approved data-deletion-request flow, keeping the row for audit. Parental biometric consent gates *disclosure* to the parent, not *collection* — collection proceeds regardless of consent status (functional necessity for plagiarism defense per TZ §5.6).

## After writing

Run the `handwriting-biometrics-auditor` agent (root `.claude/agents/`) before considering this done — the numeric thresholds here are exactly the kind of detail a quick read-through misses.

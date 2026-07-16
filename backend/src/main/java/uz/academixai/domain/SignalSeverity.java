package uz.academixai.domain;

/**
 * academix_tz.md §1.14 — drives the notify matrix (CLAUDE.md "Backend architecture"): LOW → log
 * only; MEDIUM/HIGH → class teacher + psychologist; CRITICAL → + parent.
 */
public enum SignalSeverity {
  LOW,
  MEDIUM,
  HIGH,
  CRITICAL
}

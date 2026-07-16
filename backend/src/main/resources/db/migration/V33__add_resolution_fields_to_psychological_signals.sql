-- Deviation, judgment call: academix_tz.md §2.6's PUT .../resolve body accepts
-- { notes, actionTaken }, but psychological_signals (§1.14/backend_tdd.md §4.1 table 13) has no
-- columns to store either — dropping them silently on resolve would defeat the point of the
-- psychologist documenting what they did. Adding both as nullable TEXT columns.
ALTER TABLE psychological_signals ADD COLUMN resolution_notes TEXT;
ALTER TABLE psychological_signals ADD COLUMN action_taken TEXT;

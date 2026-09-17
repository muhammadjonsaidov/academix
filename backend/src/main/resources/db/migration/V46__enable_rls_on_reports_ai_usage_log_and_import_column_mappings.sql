-- Three tenant tables that already carried school_id, and whose every read and write path was
-- already inside a tenant scope. They sat on TenantRlsCoverageTest's NOT_YET_PROTECTED list only
-- because the policy was the last step of their slice rather than the first. Each entry named the
-- concrete remaining work; this migration is that work:
--
--   * reports                — ReportService is reached from AdminReportController and, through
--                              Reporting's published API, from ParentReportService. Both are HTTP
--                              request paths, so RlsTransactionFilter has already set
--                              app.current_school_id for the whole request. V36 predicted exactly
--                              this ("scoped via service-layer schoolId filtering instead"), and
--                              the filter makes that filtering unnecessary.
--   * ai_usage_log           — written by JpaHomeworkAnalysisStore/JpaExamAnalysisStore, which run
--                              inside the homework and exam queue listeners'
--                              TenantScope.runAsTenant, and read by AdminAnalyticsService from an
--                              HTTP request. No scheduled job touches this table.
--   * import_column_mappings — admin bulk-import only, always a request path.
--
-- Checked before enabling rather than after: a policy on a table with even one unscoped caller
-- fails loudly at runtime (current_setting is unset, so Postgres raises) instead of quietly
-- returning the wrong tenant's rows. Every caller of every one of the three is inside
-- TenantScope.runAsTenant today. Same policy shape as the ten tables that came before it, so a
-- reader has one pattern to look for.

ALTER TABLE reports ENABLE ROW LEVEL SECURITY;
CREATE POLICY school_isolation ON reports
  USING (school_id = current_setting('app.current_school_id')::uuid);

ALTER TABLE ai_usage_log ENABLE ROW LEVEL SECURITY;
CREATE POLICY school_isolation ON ai_usage_log
  USING (school_id = current_setting('app.current_school_id')::uuid);

ALTER TABLE import_column_mappings ENABLE ROW LEVEL SECURITY;
CREATE POLICY school_isolation ON import_column_mappings
  USING (school_id = current_setting('app.current_school_id')::uuid);

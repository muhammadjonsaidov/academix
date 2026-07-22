/** Paged envelope returned by list endpoints that support `page` (0-based) and
 * `size` (10/20/50/100, default 20) query params — currently /teacher/submissions,
 * /admin/students, /psychologist/signals, /student/xp-history. */
export interface PageResponse<T> {
  items: T[];
  totalItems: number;
  page: number;
  size: number;
}

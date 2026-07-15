package uz.academixai.interfaces.web.admin;

/** academix_tz.md §2.2 — one entry per failed row in the bulk-import commit response. */
public record ImportRowError(int row, String field, String value, String reason) {}

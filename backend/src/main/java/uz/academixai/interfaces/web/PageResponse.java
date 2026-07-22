package uz.academixai.interfaces.web;

import java.util.List;

/**
 * Shared paged-list envelope (deviation — academix_tz.md's list endpoints are all unpaged
 * arrays; pagination added as a performance layer, size menu 10/20/50/100 on the frontend).
 * {@code totalItems} is the post-filter count so the client can render page controls.
 */
public record PageResponse<T>(List<T> items, long totalItems, int page, int size) {

  private static final int MAX_SIZE = 100;

  public static int clampSize(int size) {
    if (size <= 0) return 20;
    return Math.min(size, MAX_SIZE);
  }

  public static int clampPage(int page) {
    return Math.max(page, 0);
  }

  /** In-memory slice for services that already hold the filtered list. */
  public static <T> PageResponse<T> slice(List<T> all, int page, int size) {
    int safeSize = clampSize(size);
    int safePage = clampPage(page);
    int from = Math.min(safePage * safeSize, all.size());
    int to = Math.min(from + safeSize, all.size());
    return new PageResponse<>(all.subList(from, to), all.size(), safePage, safeSize);
  }
}

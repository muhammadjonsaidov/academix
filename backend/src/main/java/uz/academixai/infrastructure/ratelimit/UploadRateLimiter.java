package uz.academixai.infrastructure.ratelimit;

import java.time.Duration;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * academix_tz.md §5.3 — "File upload: 10 ta/daqiqa/foydalanuvchi".
 *
 * <p>One class rather than the constant duplicated in each uploading service, so both upload paths
 * share a single key namespace: otherwise a user gets 10 homework uploads AND 10 exam uploads per
 * minute by alternating endpoints, which is not what the spec's per-user limit means.
 *
 * <p><b>Bulk exam upload counts as one unit, not one per file</b> (deliberate, documented
 * deviation). {@code POST /teacher/exams/{id}/submissions/bulk} exists precisely to upload a whole
 * class's scanned papers in one request — 30 images is the normal case, so charging per image would
 * make the endpoint's own documented purpose impossible on the first use. The limit's job here is
 * to bound abuse, and that path is already TEACHER-gated, bounded by class size, and additionally
 * metered by the exam AI budget's pre-flight estimate. The student homework path — unauthenticated
 * abuse potential, one file per request, the actual disk-fill vector — is charged per request as
 * the spec intends.
 */
@Component
public class UploadRateLimiter {

  private static final int MAX_UPLOADS = 10;
  private static final Duration WINDOW = Duration.ofMinutes(1);

  private final RedisRateLimiter rateLimiter;

  public UploadRateLimiter(RedisRateLimiter rateLimiter) {
    this.rateLimiter = rateLimiter;
  }

  /** Charges one upload against {@code userId}, throwing {@code ERR_RATE_LIMIT} past the limit. */
  public void enforce(UUID userId) {
    rateLimiter.enforce(
        "upload_rate:" + userId,
        MAX_UPLOADS,
        WINDOW,
        "Juda ko'p fayl yuklandi. Bir daqiqadan keyin qayta urinib ko'ring.",
        "Daqiqasiga " + MAX_UPLOADS + " tagacha fayl yuklash mumkin.");
  }
}

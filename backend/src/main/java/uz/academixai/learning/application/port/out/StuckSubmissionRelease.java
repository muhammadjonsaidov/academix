package uz.academixai.learning.application.port.out;

import java.time.LocalDateTime;

/** Hands homework the AI never finished back to the teacher, across every school. */
public interface StuckSubmissionRelease {

  /**
   * Moves every submission still {@code SUBMITTED} or {@code AI_PROCESSING} that was handed in
   * before {@code cutoff} to {@code AI_SKIPPED}. Returns how many were moved.
   */
  int releaseSubmittedBefore(LocalDateTime cutoff);
}

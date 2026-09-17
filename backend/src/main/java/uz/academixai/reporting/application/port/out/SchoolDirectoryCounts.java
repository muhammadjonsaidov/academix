package uz.academixai.reporting.application.port.out;

import java.util.UUID;

/**
 * Outbound port for the setup counts on the admin dashboard.
 *
 * <p>One call rather than four: they are read together, always for the same school, and four
 * one-line methods would only spread one screen's data across four round trips.
 */
public interface SchoolDirectoryCounts {

  record Counts(int classes, int students, int teachers, int subjects) {}

  Counts of(UUID schoolId);
}

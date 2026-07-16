package uz.academixai.domain;

import java.util.List;

/**
 * academix_tz.md §1.18 — {@code lesson_plans.ai_generated_plan}'s JSON structure. The spec marks
 * this field "// JSON struktura" but doesn't define its shape; this is a judgment call (see
 * ROADMAP.md Sprint 3), kept deliberately simple: objectives, timed activities, materials, and a
 * homework suggestion.
 */
public record LessonPlanContent(
    List<String> objectives,
    List<LessonActivity> activities,
    List<String> materials,
    String homeworkSuggestion) {}

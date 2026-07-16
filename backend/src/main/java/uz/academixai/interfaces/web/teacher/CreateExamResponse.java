package uz.academixai.interfaces.web.teacher;

import java.util.UUID;

/** academix_tz.md §2.3 — {@code warning} is null unless the pre-flight estimate crosses 70%. */
public record CreateExamResponse(
    UUID examId, int estimatedAiCalls, int remainingExamBudget, String warning) {}

package uz.academixai.domain;

/**
 * academix_tz.md §1.11 — one entry per solution step, stored as part of {@code
 * ai_feedbacks.step_analyses} JSONB. {@code suggestion} is part of the canonical §1.11 shape but
 * not populated by the current Qwen prompt (§3.2's response example omits it) — left null until a
 * future prompt revision adds it.
 */
public record StepAnalysis(
    int stepNumber,
    String stepContent,
    boolean isCorrect,
    String errorDescription,
    String suggestion) {}

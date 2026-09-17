package uz.academixai.shared.ai;

/** A named, weighted criterion supplied to a grading request. */
public record GradingCriterion(String name, int weightPercent) {}

package uz.academixai.intelligence.domain;

/** One weighted criterion supplied to an AI grading provider. */
public record GradingCriterion(String name, int weightPercent) {}

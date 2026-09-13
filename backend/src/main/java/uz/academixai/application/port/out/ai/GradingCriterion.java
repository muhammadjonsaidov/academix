package uz.academixai.application.port.out.ai;

/** A named, weighted criterion supplied to a grading request. */
public record GradingCriterion(String name, int weightPercent) {}

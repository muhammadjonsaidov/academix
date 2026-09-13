package uz.academixai.infrastructure.ai;

import java.util.List;
import uz.academixai.domain.CriteriaScore;
import uz.academixai.domain.StepAnalysis;

/**
 * academix_tz.md §3.2 — combined grading + plagiarism, one AI-provider call (cost-optimization: was
 * 2 calls, now 1). Deliberately has no {@code aiScorePercent} — the AI never computes a final
 * percentage; {@code AIAnalysisService} derives it from {@code criteriaScores} via weighted-sum for
 * auditability/consistency.
 */
public record AiGradingResult(
    List<CriteriaScore> criteriaScores,
    String feedback,
    List<StepAnalysis> stepAnalyses,
    double plagiarismScore,
    String plagiarismType,
    String plagiarismEvidence) {}

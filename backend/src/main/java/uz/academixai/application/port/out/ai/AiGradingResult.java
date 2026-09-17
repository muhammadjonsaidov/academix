package uz.academixai.application.port.out.ai;

import java.util.List;
import uz.academixai.domain.CriteriaScore;
import uz.academixai.domain.StepAnalysis;

/** Provider output for combined grading and plagiarism analysis. */
public record AiGradingResult(
    List<CriteriaScore> criteriaScores,
    String feedback,
    List<StepAnalysis> stepAnalyses,
    double plagiarismScore,
    String plagiarismType,
    String plagiarismEvidence) {}

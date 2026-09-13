package uz.academixai.intelligence.application.port.out;

import java.util.List;
import uz.academixai.intelligence.domain.GradingAnalysis;
import uz.academixai.intelligence.domain.GradingCriterion;

/** Combined grading and plagiarism provider boundary. */
public interface GradingAi {

  GradingAnalysis grade(String subjectAndGrade, List<GradingCriterion> criteria, String text);
}

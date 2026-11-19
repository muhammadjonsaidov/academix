package uz.academixai.intelligence.domain;

import java.util.List;
import uz.academixai.domain.SubjectType;
import uz.academixai.shared.ai.GradingMode;

/**
 * Which checking mode a subject uses, and the rubric a teacher gets before setting their own
 * criteria. Teachers' configured criteria always win over these defaults.
 */
public final class GradingModes {

  private GradingModes() {}

  public static GradingMode forSubject(SubjectType type) {
    return GradingMode.forSubject(type);
  }

  public static List<GradingCriterion> defaultCriteria(GradingMode mode) {
    return switch (mode) {
      case PROBLEM_SOLVING ->
          List.of(
              new GradingCriterion("Yechish usuli", 40),
              new GradingCriterion("Javob to'g'riligi", 30),
              new GradingCriterion("Tushunarlilik", 30));
      case LANGUAGE ->
          List.of(
              new GradingCriterion("Imlo", 35),
              new GradingCriterion("Grammatika va tinish belgilari", 35),
              new GradingCriterion("Mazmun va so'z boyligi", 30));
      case WRITTEN_RESPONSE ->
          List.of(
              new GradingCriterion("Faktlar aniqligi", 40),
              new GradingCriterion("Savolga to'liq javob", 30),
              new GradingCriterion("Fikr izchilligi va dalillar", 30));
    };
  }
}

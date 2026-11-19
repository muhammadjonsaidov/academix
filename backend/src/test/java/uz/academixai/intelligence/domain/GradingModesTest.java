package uz.academixai.intelligence.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uz.academixai.domain.SubjectType;
import uz.academixai.shared.ai.GradingMode;

class GradingModesTest {

  @Test
  void everySchoolSubjectHasAMode() {
    for (SubjectType type : SubjectType.values()) {
      assertThat(GradingModes.forSubject(type)).as(type.name()).isNotNull();
    }
  }

  @Test
  void languagesAreCheckedAsLanguageAndSciencesAsProblemSolving() {
    assertThat(GradingModes.forSubject(SubjectType.LANGUAGE_UZ)).isEqualTo(GradingMode.LANGUAGE);
    assertThat(GradingModes.forSubject(SubjectType.LANGUAGE_RU)).isEqualTo(GradingMode.LANGUAGE);
    assertThat(GradingModes.forSubject(SubjectType.LANGUAGE_EN)).isEqualTo(GradingMode.LANGUAGE);
    assertThat(GradingModes.forSubject(SubjectType.MATH)).isEqualTo(GradingMode.PROBLEM_SOLVING);
    assertThat(GradingModes.forSubject(SubjectType.CHEMISTRY))
        .isEqualTo(GradingMode.PROBLEM_SOLVING);
    assertThat(GradingModes.forSubject(SubjectType.HISTORY))
        .isEqualTo(GradingMode.WRITTEN_RESPONSE);
    assertThat(GradingModes.forSubject(null)).isEqualTo(GradingMode.WRITTEN_RESPONSE);
  }

  @Test
  void everyDefaultRubricWeighsExactlyOneHundredPercent() {
    for (GradingMode mode : GradingMode.values()) {
      assertThat(
              GradingModes.defaultCriteria(mode).stream()
                  .mapToInt(GradingCriterion::weightPercent)
                  .sum())
          .as(mode.name())
          .isEqualTo(100);
    }
  }
}

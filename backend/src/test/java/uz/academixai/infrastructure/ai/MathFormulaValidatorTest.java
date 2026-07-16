package uz.academixai.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uz.academixai.domain.SubjectType;

class MathFormulaValidatorTest {

  private final MathFormulaValidator validator = new MathFormulaValidator();

  @Test
  void declaresMathSubjectType() {
    assertThat(validator.subjectType()).isEqualTo(SubjectType.MATH);
  }

  @Test
  void acceptsASolvableLinearEquation() {
    assertThat(validator.isSolvable("5x + 3 = 18 ni yeching")).isTrue();
  }

  @Test
  void acceptsAnEquationWithNoExplicitCoefficient() {
    assertThat(validator.isSolvable("x - 7 = 2")).isTrue();
  }

  @Test
  void rejectsAZeroCoefficientEquation() {
    assertThat(validator.isSolvable("0x + 5 = 10")).isFalse();
  }

  @Test
  void rejectsBlankContent() {
    assertThat(validator.isSolvable("")).isFalse();
    assertThat(validator.isSolvable(null)).isFalse();
  }

  @Test
  void acceptsContentWithNoEquationShapeAtAll() {
    // Not a linear-equation pattern (e.g. a word/geometry problem) — the validator's job is
    // screening out definitely-wrong generations, not gatekeeping every math task shape.
    assertThat(validator.isSolvable("Uchburchakning yuzini toping.")).isTrue();
  }
}

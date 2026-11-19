package uz.academixai.learning.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FivePointScaleTest {

  @Test
  void mapsPercentagesToTheSchoolScaleAtTheBoundaries() {
    assertThat(FivePointScale.fromPercent(100)).isEqualTo(5);
    assertThat(FivePointScale.fromPercent(86)).isEqualTo(5);
    assertThat(FivePointScale.fromPercent(85.4)).isEqualTo(4);
    assertThat(FivePointScale.fromPercent(71)).isEqualTo(4);
    assertThat(FivePointScale.fromPercent(70)).isEqualTo(3);
    assertThat(FivePointScale.fromPercent(56)).isEqualTo(3);
    assertThat(FivePointScale.fromPercent(55)).isEqualTo(2);
    assertThat(FivePointScale.fromPercent(0)).isEqualTo(2);
  }
}

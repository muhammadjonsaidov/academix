package uz.academixai.interfaces.web.student;

import uz.academixai.infrastructure.persistence.GradeEntity;

public record StudentGradeResponse(int score, int fivePointGrade, String teacherComment) {

  public static StudentGradeResponse from(GradeEntity entity) {
    if (entity == null) {
      return null;
    }
    var domain = entity.toDomain();
    return new StudentGradeResponse(
        domain.score(), domain.fivePointGrade(), domain.teacherComment());
  }
}

package uz.academixai.interfaces.web.student;

import uz.academixai.infrastructure.persistence.ExamGradeEntity;

public record StudentExamGradeResponse(int score, int fivePointGrade, String teacherComment) {

  public static StudentExamGradeResponse from(ExamGradeEntity entity) {
    if (entity == null) {
      return null;
    }
    var domain = entity.toDomain();
    return new StudentExamGradeResponse(
        domain.score(), domain.fivePointGrade(), domain.teacherComment());
  }
}

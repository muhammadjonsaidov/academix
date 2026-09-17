package uz.academixai.interfaces.web.student;

import uz.academixai.domain.Grade;

public record StudentGradeResponse(int score, int fivePointGrade, String teacherComment) {

  public static StudentGradeResponse from(Grade grade) {
    if (grade == null) {
      return null;
    }
    return new StudentGradeResponse(grade.score(), grade.fivePointGrade(), grade.teacherComment());
  }
}

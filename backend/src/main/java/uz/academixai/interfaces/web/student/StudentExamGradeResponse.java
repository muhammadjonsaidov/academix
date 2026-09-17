package uz.academixai.interfaces.web.student;

import uz.academixai.domain.ExamGrade;

public record StudentExamGradeResponse(int score, int fivePointGrade, String teacherComment) {

  public static StudentExamGradeResponse from(ExamGrade grade) {
    if (grade == null) {
      return null;
    }
    return new StudentExamGradeResponse(
        grade.score(), grade.fivePointGrade(), grade.teacherComment());
  }
}

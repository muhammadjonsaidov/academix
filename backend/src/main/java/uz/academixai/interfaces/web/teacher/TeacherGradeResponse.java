package uz.academixai.interfaces.web.teacher;

import uz.academixai.domain.Grade;

public record TeacherGradeResponse(
    int score, int fivePointGrade, String teacherComment, boolean teacherOverrodeAI) {

  public static TeacherGradeResponse from(Grade domain) {
    if (domain == null) {
      return null;
    }
    return new TeacherGradeResponse(
        domain.score(),
        domain.fivePointGrade(),
        domain.teacherComment(),
        domain.teacherOverrodeAI());
  }
}

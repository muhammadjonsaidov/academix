package uz.academixai.interfaces.web.teacher;

import java.util.UUID;
import uz.academixai.infrastructure.persistence.GradeRepository.ClassProgressRow;

public record TeacherClassProgressResponse(
    UUID classId, String className, int studentCount, double avgScore, long gradedCount) {

  public static TeacherClassProgressResponse from(ClassProgressRow row) {
    return new TeacherClassProgressResponse(
        row.getClassId(),
        row.getClassName(),
        row.getStudentCount(),
        Math.round(row.getAvgScore() * 10) / 10.0,
        row.getGradedCount());
  }
}

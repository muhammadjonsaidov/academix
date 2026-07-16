package uz.academixai.interfaces.web.admin;

import java.util.UUID;
import uz.academixai.infrastructure.persistence.GradeRepository.ClassProgressRow;

public record ClassProgressResponse(
    UUID classId, String className, int studentCount, double avgScore, long gradedCount) {

  public static ClassProgressResponse from(ClassProgressRow row) {
    return new ClassProgressResponse(
        row.getClassId(),
        row.getClassName(),
        row.getStudentCount(),
        Math.round(row.getAvgScore() * 10) / 10.0,
        row.getGradedCount());
  }
}

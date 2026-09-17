package uz.academixai.interfaces.web.teacher;

import java.util.UUID;
import uz.academixai.learning.application.port.out.TeacherGradingStatistics.ClassProgress;

public record TeacherClassProgressResponse(
    UUID classId, String className, int studentCount, double avgScore, long gradedCount) {

  public static TeacherClassProgressResponse from(ClassProgress row) {
    return new TeacherClassProgressResponse(
        row.classId(),
        row.className(),
        row.studentCount(),
        Math.round(row.avgScore() * 10) / 10.0,
        row.gradedCount());
  }
}

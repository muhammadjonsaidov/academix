package uz.academixai.interfaces.web.admin;

import java.util.UUID;
import uz.academixai.infrastructure.persistence.GradeRepository.TeacherRankingRow;

public record TeacherRankingResponse(
    UUID teacherId, String firstName, String lastName, double avgGrade, long gradedCount) {

  public static TeacherRankingResponse from(TeacherRankingRow row) {
    return new TeacherRankingResponse(
        row.getTeacherId(),
        row.getFirstName(),
        row.getLastName(),
        Math.round(row.getAvgGrade() * 10) / 10.0,
        row.getGradedCount());
  }
}

package uz.academixai.learning.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import uz.academixai.domain.Exam;

/** Published Learning use cases for teacher-created exams. */
public interface ExamManagement {

  record CreateResult(Exam exam, int estimatedAiCalls, int remainingExamBudget, String warning) {}

  record ExamSummary(Exam exam, int submissionsCount, int gradedCount) {}

  CreateResult create(
      UUID schoolId,
      UUID teacherId,
      UUID classId,
      UUID subjectId,
      String title,
      LocalDate examDate,
      int maxScore);

  List<ExamSummary> listWithCounts(UUID schoolId, UUID teacherId, UUID classId, UUID subjectId);

  Exam get(UUID schoolId, UUID examId);
}

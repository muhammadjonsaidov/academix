package uz.academixai.learning.application.port.in;

import java.util.List;
import java.util.UUID;
import uz.academixai.domain.Exam;
import uz.academixai.domain.ExamAIFeedback;
import uz.academixai.domain.ExamGrade;
import uz.academixai.domain.ExamSubmission;

/** Published student-scoped exam-result queries. */
public interface StudentExamQuery {

  record ExamListItem(Exam exam, String subjectName, ExamGrade grade) {}

  record ExamDetail(
      Exam exam, ExamSubmission submission, ExamAIFeedback feedback, ExamGrade grade) {}

  List<ExamListItem> list(UUID schoolId, UUID studentId);

  ExamDetail getDetail(UUID schoolId, UUID studentId, UUID examId);
}

package uz.academixai.progress.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.AIFeedback;
import uz.academixai.domain.Grade;
import uz.academixai.domain.HomeworkAssignment;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.StudentProfile;
import uz.academixai.progress.domain.XpHistoryEntry;

/** Persistence reads required to calculate a parent's child-progress view. */
public interface ParentProgressStore {

  Optional<StudentProfile> findStudentProfile(UUID studentId);

  List<HomeworkSubmission> findSubmissions(UUID studentId);

  Optional<HomeworkAssignment> findAssignment(UUID assignmentId);

  List<HomeworkAssignment> findAssignments(UUID schoolId, UUID classId);

  String subjectName(UUID subjectId);

  Optional<Grade> findGrade(UUID submissionId);

  Optional<AIFeedback> findAiFeedback(UUID submissionId);

  List<XpHistoryEntry> findXpHistory(UUID studentId);
}

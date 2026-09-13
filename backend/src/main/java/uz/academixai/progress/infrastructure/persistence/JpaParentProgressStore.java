package uz.academixai.progress.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.AIFeedback;
import uz.academixai.domain.Grade;
import uz.academixai.domain.HomeworkAssignment;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.StudentProfile;
import uz.academixai.infrastructure.persistence.AIFeedbackEntity;
import uz.academixai.infrastructure.persistence.AIFeedbackRepository;
import uz.academixai.infrastructure.persistence.GradeEntity;
import uz.academixai.infrastructure.persistence.GradeRepository;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentEntity;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentRepository;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionEntity;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.SubjectEntity;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.progress.application.port.out.ParentProgressStore;
import uz.academixai.progress.domain.XpHistoryEntry;

/** JPA persistence adapter for Parent Progress calculations. */
@Repository
public class JpaParentProgressStore implements ParentProgressStore {
  private final StudentProfileRepository profiles;
  private final HomeworkSubmissionRepository submissions;
  private final HomeworkAssignmentRepository assignments;
  private final SubjectRepository subjects;
  private final GradeRepository grades;
  private final AIFeedbackRepository feedback;
  private final XpHistoryRepository xp;

  public JpaParentProgressStore(
      StudentProfileRepository profiles,
      HomeworkSubmissionRepository submissions,
      HomeworkAssignmentRepository assignments,
      SubjectRepository subjects,
      GradeRepository grades,
      AIFeedbackRepository feedback,
      XpHistoryRepository xp) {
    this.profiles = profiles;
    this.submissions = submissions;
    this.assignments = assignments;
    this.subjects = subjects;
    this.grades = grades;
    this.feedback = feedback;
    this.xp = xp;
  }

  public Optional<StudentProfile> findStudentProfile(UUID id) {
    return profiles.findByUserId(id).map(StudentProfileEntity::toDomain);
  }

  public List<HomeworkSubmission> findSubmissions(UUID id) {
    return submissions.findByStudentIdOrderBySubmittedAtDesc(id).stream()
        .map(HomeworkSubmissionEntity::toDomain)
        .toList();
  }

  public Optional<HomeworkAssignment> findAssignment(UUID id) {
    return assignments.findById(id).map(HomeworkAssignmentEntity::toDomain);
  }

  public List<HomeworkAssignment> findAssignments(UUID school, UUID clazz) {
    return assignments.findBySchoolIdAndClassIdOrderByDeadlineAtDesc(school, clazz).stream()
        .map(HomeworkAssignmentEntity::toDomain)
        .toList();
  }

  public String subjectName(UUID id) {
    return subjects.findById(id).map(SubjectEntity::getName).orElse("Fan");
  }

  public Optional<Grade> findGrade(UUID id) {
    return grades.findBySubmissionId(id).map(GradeEntity::toDomain);
  }

  public Optional<AIFeedback> findAiFeedback(UUID id) {
    return feedback.findBySubmissionId(id).map(AIFeedbackEntity::toDomain);
  }

  public List<XpHistoryEntry> findXpHistory(UUID id) {
    return xp.findByStudentIdOrderByOccurredAtDesc(id).stream()
        .map(XpHistoryEntity::toDomain)
        .toList();
  }
}

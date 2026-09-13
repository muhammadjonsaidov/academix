package uz.academixai.intelligence.infrastructure.school;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.SubjectType;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentRepository;
import uz.academixai.infrastructure.persistence.SubjectEntity;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.intelligence.application.port.out.AssignmentSubjectLookup;

/** JPA adapter for the tutor's assignment-to-subject relevance check. */
@Repository
public class JpaAssignmentSubjectLookup implements AssignmentSubjectLookup {

  private final HomeworkAssignmentRepository assignments;
  private final SubjectRepository subjects;

  public JpaAssignmentSubjectLookup(
      HomeworkAssignmentRepository assignments, SubjectRepository subjects) {
    this.assignments = assignments;
    this.subjects = subjects;
  }

  @Override
  public Optional<SubjectType> subjectType(UUID schoolId, UUID assignmentId) {
    return assignments
        .findByIdAndSchoolId(assignmentId, schoolId)
        .flatMap(assignment -> subjects.findByIdAndSchoolId(assignment.getSubjectId(), schoolId))
        .map(SubjectEntity::toDomain)
        .map(subject -> subject.type());
  }
}

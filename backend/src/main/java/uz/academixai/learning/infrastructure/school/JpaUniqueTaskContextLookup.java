package uz.academixai.learning.infrastructure.school;

import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.SubjectEntity;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.learning.application.port.out.UniqueTaskContextLookup;

/** Transitional School catalog adapter for generated-task prompts and deterministic validation. */
@Repository
public class JpaUniqueTaskContextLookup implements UniqueTaskContextLookup {

  private final SubjectRepository subjects;
  private final SchoolClassRepository classes;

  public JpaUniqueTaskContextLookup(SubjectRepository subjects, SchoolClassRepository classes) {
    this.subjects = subjects;
    this.classes = classes;
  }

  @Override
  public Context find(UUID subjectId, UUID classId) {
    SubjectEntity subject = subjects.findById(subjectId).orElse(null);
    Integer grade = classes.findById(classId).map(SchoolClassEntity::getGrade).orElse(null);
    String subjectName = subject == null ? "Fan" : subject.getName();
    return new Context(
        grade == null ? subjectName : subjectName + " " + grade + "-sinf",
        subject == null ? null : subject.getType());
  }
}

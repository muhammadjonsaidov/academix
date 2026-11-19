package uz.academixai.learning.infrastructure.school;

import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.learning.application.port.out.SubjectNameLookup;
import uz.academixai.shared.ai.GradingMode;

/** Transitional School catalog adapter for a homework card's subject label. */
@Repository
public class JpaSubjectNameLookup implements SubjectNameLookup {

  private final SubjectRepository subjects;

  public JpaSubjectNameLookup(SubjectRepository subjects) {
    this.subjects = subjects;
  }

  @Override
  public String name(UUID subjectId) {
    return subjects.findById(subjectId).map(subject -> subject.getName()).orElse("Fan");
  }

  @Override
  public GradingMode gradingMode(UUID subjectId) {
    return GradingMode.forSubject(
        subjects.findById(subjectId).map(subject -> subject.getType()).orElse(null));
  }
}

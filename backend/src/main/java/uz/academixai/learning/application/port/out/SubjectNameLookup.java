package uz.academixai.learning.application.port.out;

import java.util.UUID;

/** School catalog read needed by the student homework card. */
public interface SubjectNameLookup {

  String name(UUID subjectId);
}

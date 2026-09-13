package uz.academixai.learning.application.port.out;

import java.util.List;
import java.util.UUID;

/** School read model used to generate one task for every student in a class. */
public interface ClassStudentQuery {

  List<UUID> studentIds(UUID schoolId, UUID classId);
}

package uz.academixai.learning.application.port.out;

import java.util.Optional;
import java.util.UUID;

/** School membership lookup used to authorize a student against a homework class. */
public interface StudentClassLookup {

  Optional<UUID> classId(UUID schoolId, UUID studentId);
}

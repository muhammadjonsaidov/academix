package uz.academixai.learning.application.port.out;

import java.util.UUID;

/** Narrow Identity read used by the teacher review screen. */
public interface StudentNameLookup {

  String fullName(UUID studentId);
}

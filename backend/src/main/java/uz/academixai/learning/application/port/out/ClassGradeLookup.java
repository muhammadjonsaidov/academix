package uz.academixai.learning.application.port.out;

import java.util.Optional;
import java.util.UUID;

/** Outbound port for a class' grade (year group), used to build the "Fan 7-sinf" AI prompt. */
public interface ClassGradeLookup {

  Optional<Integer> gradeOf(UUID classId);
}

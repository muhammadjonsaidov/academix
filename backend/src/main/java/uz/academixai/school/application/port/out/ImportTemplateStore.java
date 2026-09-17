package uz.academixai.school.application.port.out;

import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.ImportColumnMapping;

/** Outbound port for the per-school saved column-mapping template. */
public interface ImportTemplateStore {

  Optional<ImportColumnMapping> findForSchool(UUID schoolId);

  void save(ImportColumnMapping mapping);
}

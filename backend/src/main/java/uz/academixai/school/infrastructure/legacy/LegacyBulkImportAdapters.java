package uz.academixai.school.infrastructure.legacy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.domain.ImportColumnMapping;
import uz.academixai.infrastructure.importing.ExcelImportParser;
import uz.academixai.infrastructure.persistence.ImportColumnMappingEntity;
import uz.academixai.infrastructure.persistence.ImportColumnMappingRepository;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.TempImportFileStore;
import uz.academixai.school.application.port.out.ClassByNameLookup;
import uz.academixai.school.application.port.out.ImportFileStore;
import uz.academixai.school.application.port.out.ImportTemplateStore;
import uz.academixai.school.application.port.out.SpreadsheetImport;

/**
 * The four bulk-import adapters, grouped because each is a two-line delegate and four separate
 * classes would be four files of constructor ceremony around the same idea.
 */
@Component
public class LegacyBulkImportAdapters
    implements SpreadsheetImport, ImportFileStore, ImportTemplateStore, ClassByNameLookup {

  private final ExcelImportParser excel;
  private final TempImportFileStore tempFiles;
  private final ImportColumnMappingRepository mappings;
  private final SchoolClassRepository classes;

  public LegacyBulkImportAdapters(
      ExcelImportParser excel,
      TempImportFileStore tempFiles,
      ImportColumnMappingRepository mappings,
      SchoolClassRepository classes) {
    this.excel = excel;
    this.tempFiles = tempFiles;
    this.mappings = mappings;
    this.classes = classes;
  }

  @Override
  public List<String> readHeaders(byte[] fileBytes) {
    return excel.readHeaders(fileBytes);
  }

  @Override
  public List<Map<String, String>> readRows(
      byte[] fileBytes, Map<String, String> columnMapping, Integer limit) {
    return excel.readRows(fileBytes, columnMapping, limit);
  }

  @Override
  public String save(byte[] fileBytes) {
    return tempFiles.save(fileBytes);
  }

  @Override
  public Optional<byte[]> get(String token) {
    return tempFiles.get(token);
  }

  @Override
  public Optional<ImportColumnMapping> findForSchool(UUID schoolId) {
    return mappings.findBySchoolId(schoolId).map(ImportColumnMappingEntity::toDomain);
  }

  @Override
  public void save(ImportColumnMapping mapping) {
    mappings.save(ImportColumnMappingEntity.fromDomain(mapping));
  }

  @Override
  public Optional<UUID> classIdOf(UUID schoolId, String fullName) {
    return classes
        .findBySchoolIdAndFullNameIgnoreCase(schoolId, fullName)
        .map(SchoolClassEntity::getId);
  }
}

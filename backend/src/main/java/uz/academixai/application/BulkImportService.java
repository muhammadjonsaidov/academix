package uz.academixai.application;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uz.academixai.domain.ImportColumnMapping;
import uz.academixai.infrastructure.importing.ColumnMappingSuggester;
import uz.academixai.infrastructure.importing.ExcelImportParser;
import uz.academixai.infrastructure.persistence.ImportColumnMappingEntity;
import uz.academixai.infrastructure.persistence.ImportColumnMappingRepository;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.TempImportFileStore;
import uz.academixai.interfaces.web.ApiException;
import uz.academixai.interfaces.web.admin.ImportAnalyzeResponse;
import uz.academixai.interfaces.web.admin.ImportCommitResponse;
import uz.academixai.interfaces.web.admin.ImportRowError;
import uz.academixai.school.application.StudentManagementService;

/**
 * academix_tz.md §1.24/§2.2 — 2-phase, school-flexible bulk student import. See the
 * bulk-import-wizard skill for the full contract and rules.
 */
@Service
public class BulkImportService {

  private static final int PREVIEW_ROW_LIMIT = 5;

  private final ExcelImportParser excelParser;
  private final ColumnMappingSuggester mappingSuggester;
  private final TempImportFileStore tempFileStore;
  private final ImportColumnMappingRepository mappingRepository;
  private final SchoolClassRepository classRepository;
  private final StudentManagementService studentService;

  public BulkImportService(
      ExcelImportParser excelParser,
      ColumnMappingSuggester mappingSuggester,
      TempImportFileStore tempFileStore,
      ImportColumnMappingRepository mappingRepository,
      SchoolClassRepository classRepository,
      StudentManagementService studentService) {
    this.excelParser = excelParser;
    this.mappingSuggester = mappingSuggester;
    this.tempFileStore = tempFileStore;
    this.mappingRepository = mappingRepository;
    this.classRepository = classRepository;
    this.studentService = studentService;
  }

  public ImportAnalyzeResponse analyze(UUID schoolId, MultipartFile file) {
    byte[] bytes = readBytes(file);
    List<String> detectedColumns = excelParser.readHeaders(bytes);
    String fileToken = tempFileStore.save(bytes);

    Map<String, String> suggestedMapping =
        mappingRepository
            .findBySchoolId(schoolId)
            .map(ImportColumnMappingEntity::getMapping)
            // No saved template yet — heuristic suggestion (see ColumnMappingSuggester for why
            // this isn't a live Qwen call).
            .orElseGet(() -> mappingSuggester.suggest(detectedColumns));

    List<Map<String, String>> previewRows =
        excelParser.readRows(bytes, suggestedMapping, PREVIEW_ROW_LIMIT);

    return new ImportAnalyzeResponse(fileToken, detectedColumns, suggestedMapping, previewRows);
  }

  // Deliberately NOT @Transactional here — the whole HTTP request is already inside one
  // transaction (RlsTransactionFilter), so adding another REQUIRED-propagation transaction here
  // would just join it, same problem either way. What actually makes partial success work is
  // StudentManagementService.create() using REQUIRES_NEW — see its comment for why.
  public ImportCommitResponse commit(
      UUID schoolId,
      String fileToken,
      Map<String, String> columnMapping,
      boolean saveMappingAsTemplate) {
    byte[] bytes =
        tempFileStore
            .get(fileToken)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_IMPORT_FILE_NOT_FOUND",
                        "Import fayli topilmadi yoki muddati tugagan.",
                        "Faylni qaytadan yuklang."));

    List<Map<String, String>> rows = excelParser.readRows(bytes, columnMapping, null);
    List<ImportRowError> errors = new ArrayList<>();
    int imported = 0;

    for (int i = 0; i < rows.size(); i++) {
      ImportRowError error = importRow(schoolId, i, rows.get(i));
      if (error == null) {
        imported++;
      } else {
        errors.add(error);
      }
    }

    if (saveMappingAsTemplate) {
      saveTemplate(schoolId, columnMapping);
    }

    return new ImportCommitResponse(rows.size(), imported, errors.size(), errors);
  }

  private ImportRowError importRow(UUID schoolId, int rowIndex, Map<String, String> row) {
    String firstName = row.getOrDefault("firstName", "").trim();
    if (firstName.isBlank()) {
      return new ImportRowError(rowIndex, "firstName", firstName, "MISSING_REQUIRED_FIELD");
    }
    String lastName = row.getOrDefault("lastName", "").trim();
    if (lastName.isBlank()) {
      return new ImportRowError(rowIndex, "lastName", lastName, "MISSING_REQUIRED_FIELD");
    }
    String phone = row.getOrDefault("phone", "").trim();
    if (phone.isBlank()) {
      return new ImportRowError(rowIndex, "phone", phone, "MISSING_REQUIRED_FIELD");
    }
    String classNameRaw = row.getOrDefault("classId", "").trim();
    if (classNameRaw.isBlank()) {
      return new ImportRowError(rowIndex, "classId", classNameRaw, "MISSING_REQUIRED_FIELD");
    }
    String birthDateRaw = row.getOrDefault("birthDate", "").trim();
    if (birthDateRaw.isBlank()) {
      return new ImportRowError(rowIndex, "birthDate", birthDateRaw, "MISSING_REQUIRED_FIELD");
    }

    LocalDate birthDate;
    try {
      birthDate = LocalDate.parse(birthDateRaw);
    } catch (DateTimeParseException e) {
      return new ImportRowError(rowIndex, "birthDate", birthDateRaw, "INVALID_DATE");
    }

    SchoolClassEntity schoolClass =
        classRepository.findBySchoolIdAndFullNameIgnoreCase(schoolId, classNameRaw).orElse(null);
    if (schoolClass == null) {
      return new ImportRowError(rowIndex, "classId", classNameRaw, "CLASS_NOT_FOUND");
    }

    String studentNumber = row.getOrDefault("studentNumber", "").trim();

    try {
      studentService.create(
          schoolId,
          firstName,
          lastName,
          phone,
          schoolClass.getId(),
          studentNumber.isBlank() ? null : studentNumber,
          birthDate);
      return null;
    } catch (ApiException e) {
      return switch (e.getCode()) {
        case "ERR_DUPLICATE_PHONE" ->
            new ImportRowError(rowIndex, "phone", phone, "DUPLICATE_PHONE");
        case "ERR_CLASS_NOT_FOUND" ->
            new ImportRowError(rowIndex, "classId", classNameRaw, "CLASS_NOT_FOUND");
        default -> throw e;
      };
    }
  }

  private void saveTemplate(UUID schoolId, Map<String, String> columnMapping) {
    ImportColumnMappingEntity existing = mappingRepository.findBySchoolId(schoolId).orElse(null);
    UUID id = existing != null ? existing.toDomain().id() : UUID.randomUUID();
    ImportColumnMapping updated =
        new ImportColumnMapping(id, schoolId, columnMapping, LocalDateTime.now());
    mappingRepository.save(ImportColumnMappingEntity.fromDomain(updated));
  }

  private byte[] readBytes(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}

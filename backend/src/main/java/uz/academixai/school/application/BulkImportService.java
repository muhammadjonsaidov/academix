package uz.academixai.school.application;

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
import uz.academixai.school.application.port.out.ClassByNameLookup;
import uz.academixai.school.application.port.out.ImportFileStore;
import uz.academixai.school.application.port.out.ImportTemplateStore;
import uz.academixai.school.application.port.out.SpreadsheetImport;
import uz.academixai.shared.error.ApiException;

/**
 * academix_tz.md §2.2 admin bulk-import wizard — analyze a spreadsheet, then commit it.
 *
 * <p>Moved here from the legacy {@code application} package, and the reason it was left for last is
 * the reason it is worth reading: the service returned three web response records ({@code
 * ImportAnalyzeResponse}, {@code ImportCommitResponse}, {@code ImportRowError}) defined under
 * {@code interfaces.web.admin}, so the application layer depended on the delivery layer. They are
 * nested records here now — same JSON shape, owned by the use case that produces them — and the
 * controller imports them from this class.
 *
 * <p>{@code ColumnMappingSuggester} moved to {@code school.domain}: it is a pure, stateless synonym
 * table, so it was never infrastructure in the first place.
 *
 * <p>Partial success is deliberate and unchanged: rows that fail validation are reported and the
 * rest are imported. What makes that work is {@code StudentManagementService.create()} using
 * REQUIRES_NEW, and this method deliberately carries no {@code @Transactional} of its own for the
 * same reason the original did not.
 */
@Service
public class BulkImportService {

  private static final int PREVIEW_ROW_LIMIT = 5;

  private final SpreadsheetImport spreadsheet;
  private final ColumnMappingSuggester mappingSuggester;
  private final ImportFileStore tempFiles;
  private final ImportTemplateStore templates;
  private final ClassByNameLookup classes;
  private final StudentManagementService studentService;

  public BulkImportService(
      SpreadsheetImport spreadsheet,
      ColumnMappingSuggester mappingSuggester,
      ImportFileStore tempFiles,
      ImportTemplateStore templates,
      ClassByNameLookup classes,
      StudentManagementService studentService) {
    this.spreadsheet = spreadsheet;
    this.mappingSuggester = mappingSuggester;
    this.tempFiles = tempFiles;
    this.templates = templates;
    this.classes = classes;
    this.studentService = studentService;
  }

  public record ImportAnalyzeResponse(
      String fileToken,
      List<String> detectedColumns,
      Map<String, String> suggestedMapping,
      List<Map<String, String>> previewRows) {}

  public record ImportCommitResponse(
      int totalRows, int imported, int failed, List<ImportRowError> errors) {}

  public record ImportRowError(int row, String field, String value, String reason) {}

  public ImportAnalyzeResponse analyze(UUID schoolId, MultipartFile file) {
    byte[] bytes = readBytes(file);
    List<String> detectedColumns = spreadsheet.readHeaders(bytes);
    String fileToken = tempFiles.save(bytes);

    Map<String, String> suggestedMapping =
        templates
            .findForSchool(schoolId)
            .map(ImportColumnMapping::mapping)
            // No saved template yet — heuristic suggestion (see ColumnMappingSuggester for why this
            // isn't a live Qwen call).
            .orElseGet(() -> mappingSuggester.suggest(detectedColumns));

    List<Map<String, String>> previewRows =
        spreadsheet.readRows(bytes, suggestedMapping, PREVIEW_ROW_LIMIT);

    return new ImportAnalyzeResponse(fileToken, detectedColumns, suggestedMapping, previewRows);
  }

  public ImportCommitResponse commit(
      UUID schoolId,
      String fileToken,
      Map<String, String> columnMapping,
      boolean saveMappingAsTemplate) {
    byte[] bytes =
        tempFiles
            .get(fileToken)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_IMPORT_FILE_NOT_FOUND",
                        "Import fayli topilmadi yoki muddati tugagan.",
                        "Faylni qaytadan yuklang."));

    List<Map<String, String>> rows = spreadsheet.readRows(bytes, columnMapping, null);
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

    UUID classId = classes.classIdOf(schoolId, classNameRaw).orElse(null);
    if (classId == null) {
      return new ImportRowError(rowIndex, "classId", classNameRaw, "CLASS_NOT_FOUND");
    }

    String studentNumber = row.getOrDefault("studentNumber", "").trim();

    try {
      studentService.create(
          schoolId,
          firstName,
          lastName,
          phone,
          classId,
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
    ImportColumnMapping existing = templates.findForSchool(schoolId).orElse(null);
    UUID id = existing != null ? existing.id() : UUID.randomUUID();
    templates.save(new ImportColumnMapping(id, schoolId, columnMapping, LocalDateTime.now()));
  }

  private byte[] readBytes(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}

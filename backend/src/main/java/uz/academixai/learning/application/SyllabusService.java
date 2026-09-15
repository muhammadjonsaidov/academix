package uz.academixai.learning.application;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uz.academixai.interfaces.web.ApiException;
import uz.academixai.learning.application.port.out.SyllabusIngestionPublisher;
import uz.academixai.learning.application.port.out.SyllabusObjectStorage;
import uz.academixai.learning.application.port.out.SyllabusStore;
import uz.academixai.learning.domain.FileType;
import uz.academixai.learning.domain.SyllabusProcessingStatus;
import uz.academixai.learning.domain.TeacherSyllabus;

/** academix_tz.md §2.6 "Darslik" — teacher uploads, lists and fetches syllabuses. */
@Service
public class SyllabusService {

  private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
  private static final Map<String, FileType> ALLOWED_CONTENT_TYPES =
      Map.of(
          "application/pdf", FileType.PDF,
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document", FileType.DOCX,
          "image/jpeg", FileType.IMAGE,
          "image/png", FileType.IMAGE);

  private final SyllabusStore syllabuses;
  private final SyllabusObjectStorage storage;
  private final SyllabusIngestionPublisher ingestionRequests;

  public SyllabusService(
      SyllabusStore syllabuses,
      SyllabusObjectStorage storage,
      SyllabusIngestionPublisher ingestionRequests) {
    this.syllabuses = syllabuses;
    this.storage = storage;
    this.ingestionRequests = ingestionRequests;
  }

  /**
   * {@code schoolId} comes from the caller's principal rather than a users-table lookup: the school
   * is what the ingestion worker needs for its tenant scope, and the authenticated request already
   * carries it.
   */
  @Transactional
  public TeacherSyllabus upload(
      UUID schoolId,
      UUID teacherId,
      UUID subjectId,
      UUID classId,
      String title,
      MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw invalidFile("Fayl talab qilinadi.");
    }
    if (file.getSize() > MAX_FILE_BYTES) {
      throw invalidFile("Fayl hajmi 10MB dan katta.");
    }
    FileType fileType = ALLOWED_CONTENT_TYPES.get(file.getContentType());
    if (fileType == null) {
      throw invalidFile("Fayl formati noto'g'ri (PDF, DOCX, JPG yoki PNG bo'lishi kerak).");
    }
    String extension = extensionFor(fileType, file.getContentType());
    String key = "syllabuses/%s/%s.%s".formatted(teacherId, UUID.randomUUID(), extension);
    try {
      storage.upload(key, file.getBytes(), file.getContentType());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    TeacherSyllabus saved =
        syllabuses.save(
            new TeacherSyllabus(
                UUID.randomUUID(),
                teacherId,
                subjectId,
                classId,
                title,
                key,
                fileType,
                null,
                false,
                SyllabusProcessingStatus.PENDING,
                null,
                null,
                LocalDateTime.now()));
    ingestionRequests.requestIngestion(schoolId, saved.id());
    return saved;
  }

  public List<TeacherSyllabus> list(UUID teacherId, UUID subjectId, UUID classId) {
    return syllabuses.list(teacherId, subjectId, classId);
  }

  public TeacherSyllabus get(UUID teacherId, UUID syllabusId) {
    return syllabuses
        .findOwned(teacherId, syllabusId)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ERR_SYLLABUS_NOT_FOUND",
                    "Darslik topilmadi.",
                    "ID ni tekshiring."));
  }

  private static String extensionFor(FileType fileType, String contentType) {
    return switch (fileType) {
      case PDF -> "pdf";
      case DOCX -> "docx";
      case IMAGE -> "image/png".equals(contentType) ? "png" : "jpg";
    };
  }

  private static ApiException invalidFile(String message) {
    return new ApiException(
        HttpStatus.BAD_REQUEST, "ERR_INVALID_FILE", message, "Fayl tarkibini tekshiring.");
  }
}

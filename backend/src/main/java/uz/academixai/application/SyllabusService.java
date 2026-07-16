package uz.academixai.application;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uz.academixai.domain.FileType;
import uz.academixai.domain.TeacherSyllabus;
import uz.academixai.infrastructure.persistence.TeacherSyllabusEntity;
import uz.academixai.infrastructure.persistence.TeacherSyllabusRepository;
import uz.academixai.infrastructure.storage.FileStorageService;
import uz.academixai.interfaces.web.ApiException;

/**
 * academix_tz.md §1.17/§2.3 "Darslik yuklash".
 *
 * <p><b>Scope note (deliberate, see ROADMAP.md):</b> {@code extractedContent}/{@code isProcessed}
 * ("AI tomonidan chiqarilgan matn") need a text-extraction pipeline this codebase doesn't have —
 * Google Vision covers images, but PDF/DOCX text extraction needs a separate library not yet on the
 * classpath (Apache POI here is xlsx-only, from bulk-import). Deferred rather than half-built:
 * every upload lands with {@code isProcessed=false}, {@code extractedContent=null}. Lesson-plan
 * generation (Sprint 3, TZ §2.3) doesn't block on this — its request body already carries {@code
 * topic} directly from the teacher.
 */
@Service
public class SyllabusService {

  private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
  private static final Map<String, FileType> ALLOWED_CONTENT_TYPES =
      Map.of(
          "application/pdf", FileType.PDF,
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document", FileType.DOCX,
          "image/jpeg", FileType.IMAGE,
          "image/png", FileType.IMAGE);

  private final TeacherSyllabusRepository repository;
  private final FileStorageService fileStorageService;

  public SyllabusService(
      TeacherSyllabusRepository repository, FileStorageService fileStorageService) {
    this.repository = repository;
    this.fileStorageService = fileStorageService;
  }

  public TeacherSyllabus upload(
      UUID teacherId, UUID subjectId, UUID classId, String title, MultipartFile file) {
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
      fileStorageService.upload(key, file.getBytes(), file.getContentType());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    TeacherSyllabus syllabus =
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
            LocalDateTime.now());
    return repository.save(TeacherSyllabusEntity.fromDomain(syllabus)).toDomain();
  }

  public List<TeacherSyllabus> list(UUID teacherId, UUID subjectId, UUID classId) {
    var entities =
        switch ((subjectId != null ? 1 : 0) + (classId != null ? 2 : 0)) {
          case 3 ->
              repository.findByTeacherIdAndSubjectIdAndClassIdOrderByUploadedAtDesc(
                  teacherId, subjectId, classId);
          case 2 -> repository.findByTeacherIdAndClassIdOrderByUploadedAtDesc(teacherId, classId);
          case 1 ->
              repository.findByTeacherIdAndSubjectIdOrderByUploadedAtDesc(teacherId, subjectId);
          default -> repository.findByTeacherIdOrderByUploadedAtDesc(teacherId);
        };
    return entities.stream().map(TeacherSyllabusEntity::toDomain).toList();
  }

  public TeacherSyllabus get(UUID teacherId, UUID syllabusId) {
    return repository
        .findByIdAndTeacherId(syllabusId, teacherId)
        .map(TeacherSyllabusEntity::toDomain)
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

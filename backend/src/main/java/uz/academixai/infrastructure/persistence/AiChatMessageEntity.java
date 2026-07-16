package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.AiChatMessage;
import uz.academixai.domain.SubjectType;

/** JPA mapping for {@code ai_chat_messages} (V37 migration). Maps to/from {@link AiChatMessage}. */
@Entity
@Table(name = "ai_chat_messages")
public class AiChatMessageEntity {

  @Id private UUID id;

  @Column(name = "school_id", nullable = false)
  private UUID schoolId;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubjectType subject;

  @Column(nullable = false)
  private String message;

  @Column(nullable = false)
  private String response;

  @Column(name = "is_blocked")
  private boolean isBlocked;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  protected AiChatMessageEntity() {}

  public AiChatMessageEntity(
      UUID id,
      UUID schoolId,
      UUID studentId,
      SubjectType subject,
      String message,
      String response,
      boolean isBlocked,
      LocalDateTime createdAt) {
    this.id = id;
    this.schoolId = schoolId;
    this.studentId = studentId;
    this.subject = subject;
    this.message = message;
    this.response = response;
    this.isBlocked = isBlocked;
    this.createdAt = createdAt;
  }

  public static AiChatMessageEntity fromDomain(AiChatMessage domain) {
    return new AiChatMessageEntity(
        domain.id(),
        domain.schoolId(),
        domain.studentId(),
        domain.subject(),
        domain.message(),
        domain.response(),
        domain.isBlocked(),
        domain.createdAt());
  }

  public AiChatMessage toDomain() {
    return new AiChatMessage(
        id, schoolId, studentId, subject, message, response, isBlocked, createdAt);
  }
}

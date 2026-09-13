package uz.academixai.application;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.application.port.out.ai.AiProvider;
import uz.academixai.application.port.out.ai.AiProviderUnavailableException;
import uz.academixai.domain.AiChatMessage;
import uz.academixai.domain.ChatBlockReason;
import uz.academixai.domain.HomeworkAssignment;
import uz.academixai.domain.Subject;
import uz.academixai.domain.SubjectType;
import uz.academixai.infrastructure.persistence.AiChatMessageEntity;
import uz.academixai.infrastructure.persistence.AiChatMessageRepository;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentRepository;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.infrastructure.ratelimit.RedisRateLimiter;
import uz.academixai.intelligence.application.AiBudgetService;
import uz.academixai.intelligence.domain.AiCallCategory;
import uz.academixai.interfaces.web.ApiException;

/**
 * academix_tz.md §2.3/§3.4 "AI Tutor chat" — two-layer jailbreak defense: layer 1 is {@code
 * {@link AiProvider}'s system prompt, layer 2 is this service's response-level bare-answer heuristic
 * (below). Chat is the first AI category to degrade when a school's budget runs out (backend TDD
 * "AI cost/budget system") — unlike homework grading's silent {@code AI_SKIPPED}, chat has no
 * content to fall back to, so a budget-exhausted request is blocked outright with {@link
 * ChatBlockReason#BUDGET_EXHAUSTED} (a deviation — the spec's two blockReason values are both about
 * content policy, not budget).
 */
@Service
public class AiChatService {

  private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

  private static final int HISTORY_WINDOW = 50;

  // academix_tz.md §5.3 — "AI chat: 30 ta so'rov/daqiqa/foydalanuvchi". Distinct from, and not
  // replaced by, the monthly AI budget: the budget caps the SCHOOL's spend per month, so without
  // a per-user limit one student in a loop can drain the whole 20% chat sub-budget in minutes and
  // block every other student for the rest of the month. The budget's documented degradation
  // order assumes traffic is spread across users; this is what makes that true.
  private static final int MAX_CHATS_PER_MINUTE = 30;
  private static final Duration CHAT_RATE_WINDOW = Duration.ofMinutes(1);

  private final AiChatMessageRepository chatRepository;
  private final HomeworkAssignmentRepository assignmentRepository;
  private final SubjectRepository subjectRepository;
  private final AiBudgetService budgetService;
  private final AiProvider aiClient;
  private final RedisRateLimiter rateLimiter;

  public AiChatService(
      AiChatMessageRepository chatRepository,
      HomeworkAssignmentRepository assignmentRepository,
      SubjectRepository subjectRepository,
      AiBudgetService budgetService,
      AiProvider aiClient,
      RedisRateLimiter rateLimiter) {
    this.chatRepository = chatRepository;
    this.assignmentRepository = assignmentRepository;
    this.subjectRepository = subjectRepository;
    this.budgetService = budgetService;
    this.aiClient = aiClient;
    this.rateLimiter = rateLimiter;
  }

  public record ChatResult(String response, boolean isBlocked, ChatBlockReason blockReason) {}

  public ChatResult chat(
      UUID schoolId, UUID studentId, String subjectRaw, String message, UUID assignmentId) {
    // First check in the method, before any DB read or budget accounting — the cheapest possible
    // rejection, and it must precede the budget check so spam can't consume the school's quota.
    // Throws ERR_RATE_LIMIT (429) rather than returning a blocked ChatResult like the budget and
    // relevance paths do: those are conversational outcomes worth persisting to the student's
    // history, whereas flooding is a client fault, and writing 30+ spam turns into chat history
    // would be its own storage-abuse vector.
    rateLimiter.enforce(
        "ai_chat_rate:" + studentId,
        MAX_CHATS_PER_MINUTE,
        CHAT_RATE_WINDOW,
        "Juda ko'p savol yubordingiz. Bir daqiqadan keyin qayta urinib ko'ring.",
        "Daqiqasiga " + MAX_CHATS_PER_MINUTE + " tagacha savol berish mumkin.");

    SubjectType subject = parseSubject(subjectRaw);

    if (assignmentId != null && !isRelevant(schoolId, assignmentId, subject)) {
      return persistAndReturn(
          schoolId,
          studentId,
          subject,
          message,
          "Savolingiz ushbu vazifaga tegishli emasga o'xshaydi. Iltimos, vazifaga oid savol bering.",
          true,
          ChatBlockReason.IRRELEVANT_QUESTION);
    }

    if (!budgetService.isWithinAiBudget(schoolId, AiCallCategory.CHAT)) {
      return persistAndReturn(
          schoolId,
          studentId,
          subject,
          message,
          "AI Tutor hozircha band — bu oyki limitga yetdik. Iltimos keyinroq urinib ko'ring yoki o'qituvchingizdan so'rang.",
          true,
          ChatBlockReason.BUDGET_EXHAUSTED);
    }

    String subjectAndContext = "Fan: " + subject;
    String rawResponse;
    try {
      rawResponse = aiClient.tutorChat(subjectAndContext, message);
    } catch (AiProviderUnavailableException e) {
      log.warn("AI provider tutor chat unavailable for student {}", studentId, e);
      return persistAndReturn(
          schoolId,
          studentId,
          subject,
          message,
          "AI Tutor hozircha ishlamayapti. Iltimos birozdan so'ng qayta urinib ko'ring.",
          false,
          null);
    }
    budgetService.recordAiUsage(schoolId, AiCallCategory.CHAT);

    if (looksLikeBareAnswer(rawResponse)) {
      return persistAndReturn(
          schoolId,
          studentId,
          subject,
          message,
          "Keling, qadam-baqadam boshlaylik — avval qaysi qadamda qiynalayotganingizni ayting.",
          true,
          ChatBlockReason.POTENTIAL_ANSWER_LEAK);
    }

    return persistAndReturn(schoolId, studentId, subject, message, rawResponse, false, null);
  }

  public List<AiChatMessage> history(UUID studentId, String subjectRaw, int limit) {
    Limit jpaLimit = Limit.of(Math.min(Math.max(limit, 1), HISTORY_WINDOW));
    if (subjectRaw == null || subjectRaw.isBlank()) {
      return chatRepository.findByStudentIdOrderByCreatedAtDesc(studentId, jpaLimit).stream()
          .map(AiChatMessageEntity::toDomain)
          .toList();
    }
    SubjectType subject = parseSubject(subjectRaw);
    return chatRepository
        .findByStudentIdAndSubjectOrderByCreatedAtDesc(studentId, subject, jpaLimit)
        .stream()
        .map(AiChatMessageEntity::toDomain)
        .toList();
  }

  private ChatResult persistAndReturn(
      UUID schoolId,
      UUID studentId,
      SubjectType subject,
      String message,
      String response,
      boolean isBlocked,
      ChatBlockReason blockReason) {
    AiChatMessage entry =
        new AiChatMessage(
            UUID.randomUUID(),
            schoolId,
            studentId,
            subject,
            message,
            response,
            isBlocked,
            LocalDateTime.now());
    chatRepository.save(AiChatMessageEntity.fromDomain(entry));
    trimHistory(studentId, subject);
    return new ChatResult(response, isBlocked, blockReason);
  }

  // academix_tz.md §8: sliding-window at insert time, not a cron/retention job — if (student_id,
  // subject) exceeds 50, the oldest row is deleted in the same transaction.
  private void trimHistory(UUID studentId, SubjectType subject) {
    while (chatRepository.countByStudentIdAndSubject(studentId, subject) > HISTORY_WINDOW) {
      chatRepository
          .findFirstByStudentIdAndSubjectOrderByCreatedAtAsc(studentId, subject)
          .ifPresent(chatRepository::delete);
    }
  }

  // academix_tz.md §3.4 layer 2: a suspiciously short, bare number/formula response (no
  // step-by-step explanation) gets overridden regardless of what the model actually said.
  private boolean looksLikeBareAnswer(String response) {
    String trimmed = response == null ? "" : response.trim();
    if (trimmed.isEmpty()) {
      return false;
    }
    int wordCount = trimmed.split("\\s+").length;
    boolean isNumericOrFormula = trimmed.matches("^[\\d\\s+\\-*/=.,()xXyY^√%]+$");
    return wordCount <= 4 && isNumericOrFormula;
  }

  // Deterministic pre-check (academix_tz.md §2.3's context.assignmentId) — compares the
  // requested chat subject against the assignment's own subject rather than an extra AI call,
  // same "cheap deterministic check before spending an AI call" pattern as §1.9's unique-task
  // pre-check.
  private boolean isRelevant(UUID schoolId, UUID assignmentId, SubjectType subject) {
    HomeworkAssignment assignment =
        assignmentRepository
            .findByIdAndSchoolId(assignmentId, schoolId)
            .map(e -> e.toDomain())
            .orElseThrow(AiChatService::assignmentNotFound);
    Subject assignmentSubject =
        subjectRepository
            .findByIdAndSchoolId(assignment.subjectId(), schoolId)
            .map(e -> e.toDomain())
            .orElseThrow(AiChatService::assignmentNotFound);
    return assignmentSubject.type() == subject;
  }

  private static SubjectType parseSubject(String subjectRaw) {
    if (subjectRaw == null || subjectRaw.isBlank()) {
      throw invalidSubject();
    }
    // academix_tz.md §2.3's own example lists "GENERAL" alongside real SubjectType values — no
    // such enum constant exists (SubjectType has OTHER instead), so GENERAL is treated as an
    // alias for OTHER (judgment call).
    String normalized = subjectRaw.trim().toUpperCase(Locale.ROOT);
    if ("GENERAL".equals(normalized)) {
      return SubjectType.OTHER;
    }
    try {
      return SubjectType.valueOf(normalized);
    } catch (IllegalArgumentException e) {
      throw invalidSubject();
    }
  }

  private static ApiException invalidSubject() {
    return new ApiException(
        HttpStatus.BAD_REQUEST,
        "ERR_INVALID_SUBJECT",
        "Noto'g'ri fan turi.",
        "subject qiymatini tekshiring.");
  }

  private static ApiException assignmentNotFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND, "ERR_ASSIGNMENT_NOT_FOUND", "Vazifa topilmadi.", "ID ni tekshiring.");
  }
}

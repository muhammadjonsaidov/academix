package uz.academixai.intelligence.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.AiChatMessage;
import uz.academixai.domain.ChatBlockReason;
import uz.academixai.domain.SubjectType;
import uz.academixai.intelligence.application.port.in.TutorChat;
import uz.academixai.intelligence.application.port.out.AssignmentSubjectLookup;
import uz.academixai.intelligence.application.port.out.ChatAbuseGuard;
import uz.academixai.intelligence.application.port.out.TutorAi;
import uz.academixai.intelligence.application.port.out.TutorChatStore;
import uz.academixai.intelligence.domain.AiCallCategory;
import uz.academixai.interfaces.web.ApiException;

/** Intelligence-owned tutor interaction with deterministic guardrails and bounded history. */
@Service
public class TutorChatService implements TutorChat {

  private static final Logger log = LoggerFactory.getLogger(TutorChatService.class);
  private static final int HISTORY_WINDOW = 50;

  private final TutorChatStore messages;
  private final AssignmentSubjectLookup assignments;
  private final ChatAbuseGuard abuseGuard;
  private final AiBudgetService budget;
  private final TutorAi ai;

  public TutorChatService(
      TutorChatStore messages,
      AssignmentSubjectLookup assignments,
      ChatAbuseGuard abuseGuard,
      AiBudgetService budget,
      TutorAi ai) {
    this.messages = messages;
    this.assignments = assignments;
    this.abuseGuard = abuseGuard;
    this.budget = budget;
    this.ai = ai;
  }

  @Override
  public Result chat(
      UUID schoolId, UUID studentId, String subjectRaw, String message, UUID assignmentId) {
    abuseGuard.enforce(studentId);
    SubjectType subject = parseSubject(subjectRaw);
    if (assignmentId != null && !subjectMatches(schoolId, assignmentId, subject)) {
      return persist(
          schoolId,
          studentId,
          subject,
          message,
          "Savolingiz ushbu vazifaga tegishli emasga o'xshaydi. Iltimos, vazifaga oid savol bering.",
          true,
          ChatBlockReason.IRRELEVANT_QUESTION);
    }
    if (!budget.isWithinAiBudget(schoolId, AiCallCategory.CHAT)) {
      return persist(
          schoolId,
          studentId,
          subject,
          message,
          "AI Tutor hozircha band — bu oyki limitga yetdik. Iltimos keyinroq urinib ko'ring yoki o'qituvchingizdan so'rang.",
          true,
          ChatBlockReason.BUDGET_EXHAUSTED);
    }
    String response;
    try {
      response = ai.respond("Fan: " + subject, message);
    } catch (AiProviderUnavailableException exception) {
      log.warn("AI provider tutor chat unavailable for student {}", studentId, exception);
      return persist(
          schoolId,
          studentId,
          subject,
          message,
          "AI Tutor hozircha ishlamayapti. Iltimos birozdan so'ng qayta urinib ko'ring.",
          false,
          null);
    }
    budget.recordAiUsage(schoolId, AiCallCategory.CHAT);
    if (looksLikeBareAnswer(response)) {
      return persist(
          schoolId,
          studentId,
          subject,
          message,
          "Keling, qadam-baqadam boshlaylik — avval qaysi qadamda qiynalayotganingizni ayting.",
          true,
          ChatBlockReason.POTENTIAL_ANSWER_LEAK);
    }
    return persist(schoolId, studentId, subject, message, response, false, null);
  }

  @Override
  public List<AiChatMessage> history(UUID studentId, String subjectRaw, int limit) {
    int boundedLimit = Math.min(Math.max(limit, 1), HISTORY_WINDOW);
    return subjectRaw == null || subjectRaw.isBlank()
        ? messages.findRecent(studentId, null, boundedLimit)
        : messages.findRecent(studentId, parseSubject(subjectRaw), boundedLimit);
  }

  private Result persist(
      UUID schoolId,
      UUID studentId,
      SubjectType subject,
      String message,
      String response,
      boolean blocked,
      ChatBlockReason blockReason) {
    messages.save(
        new AiChatMessage(
            UUID.randomUUID(),
            schoolId,
            studentId,
            subject,
            message,
            response,
            blocked,
            LocalDateTime.now()));
    while (messages.count(studentId, subject) > HISTORY_WINDOW) {
      messages.deleteOldest(studentId, subject);
    }
    return new Result(response, blocked, blockReason);
  }

  private boolean subjectMatches(UUID schoolId, UUID assignmentId, SubjectType requestedSubject) {
    return assignments
            .subjectType(schoolId, assignmentId)
            .orElseThrow(TutorChatService::assignmentNotFound)
        == requestedSubject;
  }

  private static boolean looksLikeBareAnswer(String response) {
    String trimmed = response == null ? "" : response.trim();
    if (trimmed.isEmpty()) {
      return false;
    }
    return trimmed.split("\\s+").length <= 4 && trimmed.matches("^[\\d\\s+\\-*/=.,()xXyY^√%]+$");
  }

  private static SubjectType parseSubject(String raw) {
    if (raw == null || raw.isBlank()) {
      throw invalidSubject();
    }
    String normalized = raw.trim().toUpperCase(Locale.ROOT);
    if ("GENERAL".equals(normalized)) {
      return SubjectType.OTHER;
    }
    try {
      return SubjectType.valueOf(normalized);
    } catch (IllegalArgumentException exception) {
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

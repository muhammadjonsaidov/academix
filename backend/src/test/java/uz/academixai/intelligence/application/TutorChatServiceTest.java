package uz.academixai.intelligence.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uz.academixai.domain.AiChatMessage;
import uz.academixai.domain.ChatBlockReason;
import uz.academixai.domain.SubjectType;
import uz.academixai.intelligence.application.port.in.TutorChat;
import uz.academixai.intelligence.application.port.out.AiMonthlyLimitLookup;
import uz.academixai.intelligence.application.port.out.AiUsageCounter;
import uz.academixai.intelligence.application.port.out.AssignmentSubjectLookup;
import uz.academixai.intelligence.application.port.out.ChatAbuseGuard;
import uz.academixai.intelligence.application.port.out.TutorAi;
import uz.academixai.intelligence.application.port.out.TutorChatStore;
import uz.academixai.intelligence.domain.AiCallCategory;

class TutorChatServiceTest {

  @Test
  void unrelatedAssignmentQuestionIsBlockedWithoutCallingAi() {
    Store store = new Store();
    UUID schoolId = UUID.randomUUID();
    TutorChat chat =
        service(store, (ignored, ignoredAssignment) -> Optional.of(SubjectType.MATH), failAi());

    TutorChat.Result result =
        chat.chat(schoolId, UUID.randomUUID(), "PHYSICS", "Javobini ayting", UUID.randomUUID());

    assertThat(result.isBlocked()).isTrue();
    assertThat(result.blockReason()).isEqualTo(ChatBlockReason.IRRELEVANT_QUESTION);
    assertThat(store.messages).singleElement().extracting(AiChatMessage::isBlocked).isEqualTo(true);
  }

  @Test
  void bareAnswerIsReplacedWithGuidanceAfterRecordingUsage() {
    Store store = new Store();
    Counter counter = new Counter();
    TutorChat chat =
        service(
            store,
            (school, assignment) -> Optional.of(SubjectType.MATH),
            (context, message) -> "42",
            counter);

    TutorChat.Result result =
        chat.chat(UUID.randomUUID(), UUID.randomUUID(), "MATH", "2*21?", null);

    assertThat(result.isBlocked()).isTrue();
    assertThat(result.blockReason()).isEqualTo(ChatBlockReason.POTENTIAL_ANSWER_LEAK);
    assertThat(result.response()).contains("qadam-baqadam");
    assertThat(counter.increments).isEqualTo(1);
  }

  private static TutorChat service(Store store, AssignmentSubjectLookup assignments, TutorAi ai) {
    return service(store, assignments, ai, new Counter());
  }

  private static TutorChat service(
      Store store, AssignmentSubjectLookup assignments, TutorAi ai, Counter counter) {
    AiMonthlyLimitLookup limits = schoolId -> Optional.of(100);
    ChatAbuseGuard guard = studentId -> {};
    return new TutorChatService(
        store, assignments, guard, new AiBudgetService(limits, counter), ai);
  }

  private static TutorAi failAi() {
    return (context, message) -> {
      throw new AssertionError("AI must not be called for an unrelated assignment question");
    };
  }

  private static final class Counter implements AiUsageCounter {

    private int increments;

    @Override
    public long currentUsage(UUID schoolId, AiCallCategory category) {
      return 0;
    }

    @Override
    public void increment(UUID schoolId, AiCallCategory category) {
      increments++;
    }
  }

  private static final class Store implements TutorChatStore {

    private final List<AiChatMessage> messages = new ArrayList<>();

    @Override
    public AiChatMessage save(AiChatMessage message) {
      messages.add(message);
      return message;
    }

    @Override
    public List<AiChatMessage> findRecent(UUID studentId, SubjectType subject, int limit) {
      return messages.stream()
          .filter(message -> message.studentId().equals(studentId))
          .filter(message -> subject == null || message.subject() == subject)
          .limit(limit)
          .toList();
    }

    @Override
    public long count(UUID studentId, SubjectType subject) {
      return messages.stream()
          .filter(message -> message.studentId().equals(studentId))
          .filter(message -> message.subject() == subject)
          .count();
    }

    @Override
    public void deleteOldest(UUID studentId, SubjectType subject) {
      messages.removeIf(
          message -> message.studentId().equals(studentId) && message.subject() == subject);
    }
  }
}

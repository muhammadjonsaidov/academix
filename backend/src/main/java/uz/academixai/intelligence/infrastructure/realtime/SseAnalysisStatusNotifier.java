package uz.academixai.intelligence.infrastructure.realtime;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.infrastructure.realtime.RealtimeEventBus;
import uz.academixai.intelligence.application.port.out.AnalysisStatusNotifier;

/**
 * SSE adapter that publishes a committed homework-analysis state transition to both participants.
 */
@Component
public class SseAnalysisStatusNotifier implements AnalysisStatusNotifier {

  private final RealtimeEventBus events;

  public SseAnalysisStatusNotifier(RealtimeEventBus events) {
    this.events = events;
  }

  @Override
  public void notifyHomework(
      UUID teacherId, UUID studentId, UUID submissionId, SubmissionStatus status) {
    notify(teacherId, studentId, submissionId, "HOMEWORK", status);
  }

  @Override
  public void notifyExam(
      UUID teacherId, UUID studentId, UUID submissionId, SubmissionStatus status) {
    notify(teacherId, studentId, submissionId, "EXAM", status);
  }

  private void notify(
      UUID teacherId, UUID studentId, UUID submissionId, String type, SubmissionStatus status) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("submissionId", submissionId.toString());
    payload.put("type", type);
    payload.put("status", status.name());
    events.publishAfterCommit(teacherId, "ai.status", payload);
    events.publishAfterCommit(studentId, "ai.status", payload);
  }
}

package uz.academixai.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import uz.academixai.domain.CriteriaScore;
import uz.academixai.domain.LessonActivity;
import uz.academixai.domain.LessonPlanContent;
import uz.academixai.domain.StepAnalysis;

/**
 * academix_tz.md §3.2 — Alibaba Qwen via DashScope's OpenAI-compatible {@code /chat/completions}
 * endpoint. Only text (no image) — OCR already happened via {@link GoogleVisionClient}, so {@code
 * qwen3.7-max} (text-only) is used, not the {@code qwen3.7-plus} multimodal model (spec explicitly
 * reserves that for future image+text use, not called anywhere yet).
 *
 * <p>The spec's own JSON example has a top-level {@code "system"} field alongside {@code
 * "messages"} — that's illustrative, not the real OpenAI-compatible wire format (which puts the
 * system prompt as a {@code role: "system"} entry inside {@code messages}); this client sends the
 * real wire format, matching what "OpenAI-compatible chat-completions" actually means.
 */
@Component
@EnableConfigurationProperties(QwenProperties.class)
public class QwenAIClient {

  private static final String SYSTEM_PROMPT =
      """
      Sen maktablar uchun yordamchi o'qituvchisan. Berilgan baholash mezonlari asosida yechimni \
      qadam-baqadam tekshir, xatolarni ko'rsat, va matn mustaqil yozilganmi yoki AI orqali \
      generatsiya qilinganmi bahola. O'quvchiga to'g'ridan-to'g'ri yakuniy baho ko'rsatma, faqat \
      tahlil va mezon bo'yicha ball ber.

      Javobni FAQAT quyidagi JSON formatida qaytar, boshqa matn qo'shma:
      {
        "criteriaScores": [{"name": "...", "weightPercent": 0, "score": 0}],
        "feedback": "...",
        "stepAnalyses": [{"stepNumber": 0, "stepContent": "...", "isCorrect": true, "errorDescription": null}],
        "plagiarismScore": 0.0,
        "plagiarismType": "CLEAN|SUSPICIOUS|AI_GENERATED",
        "plagiarismEvidence": "..."
      }""";

  // academix_tz.md §1.18/§2.3 doesn't define a prompt contract for lesson-plan generation (unlike
  // §3.2's worked grading example) — judgment call, see ROADMAP.md Sprint 3.
  private static final String LESSON_PLAN_SYSTEM_PROMPT =
      """
      Sen maktab o'qituvchisi uchun dars rejasi tuzuvchi yordamchisan. Berilgan fan, sinf, mavzu \
      va (agar mavjud bo'lsa) darslik matni asosida bitta dars uchun aniq, amaliy reja tuz.

      Javobni FAQAT quyidagi JSON formatida qaytar, boshqa matn qo'shma:
      {
        "objectives": ["..."],
        "activities": [{"description": "...", "durationMinutes": 0}],
        "materials": ["..."],
        "homeworkSuggestion": "..."
      }""";

  // academix_tz.md §1.9's own worked example is a MATH linear equation; no prompt contract is
  // given, so this wording is a judgment call, see ROADMAP.md Sprint 3.
  private static final String UNIQUE_TASK_SYSTEM_PROMPT =
      """
      Sen maktab o'qituvchisi uchun har bir o'quvchiga alohida, bir xil qiyinlik darajasidagi \
      unique topshiriq tuzuvchi yordamchisan. Berilgan standart topshiriq asosida, xuddi shu \
      mavzu va qiyinlik darajasida, lekin BOSHQA raqamlar/holat bilan yangi topshiriq tuz.

      Javobni FAQAT quyidagi JSON formatida qaytar, boshqa matn qo'shma:
      {"taskContent": "..."}""";

  // Deliberately independent/context-free (academix_tz.md §1.9 step 2) — no mention of how the
  // task was generated, so a correlated failure in the generation call doesn't also corrupt this
  // check.
  private static final String VERIFY_TASK_SYSTEM_PROMPT =
      """
      Senga faqat bitta masala matni beriladi, boshqa hech qanday kontekst yo'q. Ushbu masalani \
      yechib bo'ladimi, ya'ni to'g'ri va yagona yechimi bormi, tekshir.

      Javobni FAQAT quyidagi JSON formatida qaytar, boshqa matn qo'shma:
      {"solvable": true, "reason": "..."}""";

  private final RestClient restClient;
  private final QwenProperties properties;
  private final ObjectMapper objectMapper;

  public QwenAIClient(
      RestClient.Builder restClientBuilder, QwenProperties properties, ObjectMapper objectMapper) {
    this.restClient = restClientBuilder.build();
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @CircuitBreaker(name = "qwen", fallbackMethod = "gradeSubmissionFallback")
  public QwenGradingResult gradeSubmission(
      String subjectAndGrade, List<GradingCriterion> criteria, String extractedText) {
    String userContent = buildUserContent(subjectAndGrade, criteria, extractedText);

    Map<String, Object> requestBody =
        Map.of(
            "model", properties.modelText(),
            "messages",
                List.of(
                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                    Map.of("role", "user", "content", userContent)));

    JsonNode response =
        restClient
            .post()
            .uri(properties.baseUrl() + "/chat/completions")
            .header("Authorization", "Bearer " + properties.apiKey())
            .body(requestBody)
            .retrieve()
            .body(JsonNode.class);

    return parseGradingResult(response, objectMapper);
  }

  @SuppressWarnings("unused") // invoked reflectively by resilience4j on circuit-open/failure
  private QwenGradingResult gradeSubmissionFallback(
      String subjectAndGrade,
      List<GradingCriterion> criteria,
      String extractedText,
      Throwable cause) {
    throw new QwenUnavailableException("Qwen grading unavailable", cause);
  }

  @CircuitBreaker(name = "qwen", fallbackMethod = "generateLessonPlanFallback")
  public LessonPlanContent generateLessonPlan(
      String subjectAndGrade, String topic, String syllabusExtractedContent) {
    String userContent =
        "Fan/sinf: %s. Mavzu: %s.%s"
            .formatted(
                subjectAndGrade,
                topic,
                syllabusExtractedContent == null || syllabusExtractedContent.isBlank()
                    ? ""
                    : " Darslik matni: " + syllabusExtractedContent);

    Map<String, Object> requestBody =
        Map.of(
            "model", properties.modelText(),
            "messages",
                List.of(
                    Map.of("role", "system", "content", LESSON_PLAN_SYSTEM_PROMPT),
                    Map.of("role", "user", "content", userContent)));

    JsonNode response =
        restClient
            .post()
            .uri(properties.baseUrl() + "/chat/completions")
            .header("Authorization", "Bearer " + properties.apiKey())
            .body(requestBody)
            .retrieve()
            .body(JsonNode.class);

    return parseLessonPlanContent(response, objectMapper);
  }

  @SuppressWarnings("unused") // invoked reflectively by resilience4j on circuit-open/failure
  private LessonPlanContent generateLessonPlanFallback(
      String subjectAndGrade, String topic, String syllabusExtractedContent, Throwable cause) {
    throw new QwenUnavailableException("Qwen lesson-plan generation unavailable", cause);
  }

  static LessonPlanContent parseLessonPlanContent(JsonNode response, ObjectMapper objectMapper) {
    String content = response.path("choices").path(0).path("message").path("content").asText("");
    String json = stripMarkdownFence(content);

    try {
      JsonNode root = objectMapper.readTree(json);
      List<String> objectives = new ArrayList<>();
      for (JsonNode node : root.path("objectives")) {
        objectives.add(node.asText());
      }
      List<LessonActivity> activities = new ArrayList<>();
      for (JsonNode node : root.path("activities")) {
        activities.add(
            new LessonActivity(
                node.path("description").asText(), node.path("durationMinutes").asInt()));
      }
      List<String> materials = new ArrayList<>();
      for (JsonNode node : root.path("materials")) {
        materials.add(node.asText());
      }
      return new LessonPlanContent(
          objectives, activities, materials, root.path("homeworkSuggestion").asText(""));
    } catch (Exception e) {
      throw new QwenUnavailableException("Qwen response was not valid JSON: " + content, e);
    }
  }

  @CircuitBreaker(name = "qwen", fallbackMethod = "generateUniqueTaskFallback")
  public String generateUniqueTask(String subjectAndGrade, String standardTaskDescription) {
    String userContent =
        "Fan/sinf: %s. Standart topshiriq: %s".formatted(subjectAndGrade, standardTaskDescription);

    Map<String, Object> requestBody =
        Map.of(
            "model", properties.modelText(),
            "messages",
                List.of(
                    Map.of("role", "system", "content", UNIQUE_TASK_SYSTEM_PROMPT),
                    Map.of("role", "user", "content", userContent)));

    JsonNode response =
        restClient
            .post()
            .uri(properties.baseUrl() + "/chat/completions")
            .header("Authorization", "Bearer " + properties.apiKey())
            .body(requestBody)
            .retrieve()
            .body(JsonNode.class);

    String content = response.path("choices").path(0).path("message").path("content").asText("");
    String json = stripMarkdownFence(content);
    try {
      return objectMapper.readTree(json).path("taskContent").asText("");
    } catch (Exception e) {
      throw new QwenUnavailableException("Qwen response was not valid JSON: " + content, e);
    }
  }

  @SuppressWarnings("unused") // invoked reflectively by resilience4j on circuit-open/failure
  private String generateUniqueTaskFallback(
      String subjectAndGrade, String standardTaskDescription, Throwable cause) {
    throw new QwenUnavailableException("Qwen unique-task generation unavailable", cause);
  }

  @CircuitBreaker(name = "qwen", fallbackMethod = "verifyTaskSolvableFallback")
  public boolean verifyTaskSolvable(String taskContent) {
    Map<String, Object> requestBody =
        Map.of(
            "model", properties.modelText(),
            "messages",
                List.of(
                    Map.of("role", "system", "content", VERIFY_TASK_SYSTEM_PROMPT),
                    Map.of("role", "user", "content", taskContent)));

    JsonNode response =
        restClient
            .post()
            .uri(properties.baseUrl() + "/chat/completions")
            .header("Authorization", "Bearer " + properties.apiKey())
            .body(requestBody)
            .retrieve()
            .body(JsonNode.class);

    String content = response.path("choices").path(0).path("message").path("content").asText("");
    String json = stripMarkdownFence(content);
    try {
      return objectMapper.readTree(json).path("solvable").asBoolean(false);
    } catch (Exception e) {
      throw new QwenUnavailableException("Qwen response was not valid JSON: " + content, e);
    }
  }

  @SuppressWarnings("unused") // invoked reflectively by resilience4j on circuit-open/failure
  private boolean verifyTaskSolvableFallback(String taskContent, Throwable cause) {
    throw new QwenUnavailableException("Qwen task verification unavailable", cause);
  }

  static String buildUserContent(
      String subjectAndGrade, List<GradingCriterion> criteria, String extractedText) {
    String criteriaJson =
        criteria.stream()
            .map(c -> "{name: '%s', weightPercent: %d}".formatted(c.name(), c.weightPercent()))
            .collect(Collectors.joining(", "));
    return "Fan: %s. Baholash mezonlari: [%s]. Yechim matni: %s"
        .formatted(subjectAndGrade, criteriaJson, extractedText);
  }

  /** Extracts and parses the OpenAI-shaped {@code choices[0].message.content} JSON string. */
  static QwenGradingResult parseGradingResult(JsonNode response, ObjectMapper objectMapper) {
    String content = response.path("choices").path(0).path("message").path("content").asText("");
    String json = stripMarkdownFence(content);

    try {
      JsonNode root = objectMapper.readTree(json);
      List<CriteriaScore> criteriaScores = new ArrayList<>();
      for (JsonNode node : root.path("criteriaScores")) {
        criteriaScores.add(
            new CriteriaScore(
                node.path("name").asText(),
                node.path("weightPercent").asInt(),
                node.path("score").asInt()));
      }
      List<StepAnalysis> stepAnalyses = new ArrayList<>();
      for (JsonNode node : root.path("stepAnalyses")) {
        stepAnalyses.add(
            new StepAnalysis(
                node.path("stepNumber").asInt(),
                node.path("stepContent").asText(),
                node.path("isCorrect").asBoolean(),
                node.path("errorDescription").isNull()
                    ? null
                    : node.path("errorDescription").asText(),
                null));
      }
      return new QwenGradingResult(
          criteriaScores,
          root.path("feedback").asText(""),
          stepAnalyses,
          root.path("plagiarismScore").asDouble(0),
          root.path("plagiarismType").asText("CLEAN"),
          root.path("plagiarismEvidence").asText(""));
    } catch (Exception e) {
      throw new QwenUnavailableException("Qwen response was not valid JSON: " + content, e);
    }
  }

  private static String stripMarkdownFence(String content) {
    String trimmed = content.trim();
    if (trimmed.startsWith("```")) {
      trimmed = trimmed.replaceFirst("^```(json)?", "").trim();
      if (trimmed.endsWith("```")) {
        trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
      }
    }
    return trimmed;
  }
}

package uz.academixai.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import uz.academixai.domain.CriteriaScore;
import uz.academixai.domain.LessonPlanContent;

/**
 * Pure parsing-logic test against the exact grading JSON shape documented in academix_tz.md §3.2 —
 * no network call (no real QWEN_API_KEY is available in this environment; see CLAUDE.md).
 *
 * <p>Two Jackson stacks on purpose, mirroring production: {@code jackson3Mapper} builds the outer
 * "chat completion" response fixture (Jackson 3 — matches {@code QwenAIClient}'s HTTP-body-bound
 * {@code response} parameter type, confirmed real by an actual InvalidDefinitionException when this
 * used the legacy type), while {@code objectMapper} (legacy Jackson 2, the same {@code
 * JacksonConfig} bean used at runtime) parses the extracted content string internally.
 */
class QwenAIClientTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final JsonMapper jackson3Mapper = JsonMapper.builder().build();

  private static final String GRADING_JSON =
      """
      {
        "criteriaScores": [
          { "name": "Yechish usuli", "weightPercent": 40, "score": 90 },
          { "name": "Javob to'g'riligi", "weightPercent": 30, "score": 100 },
          { "name": "Tushunarlilik", "weightPercent": 30, "score": 70 }
        ],
        "feedback": "Juda yaxshi urinish, lekin diskriminantni hisoblashda minus ishorasi xato ketgan.",
        "stepAnalyses": [
          { "stepNumber": 1, "stepContent": "5x^2 - 14x - 3 = 0", "isCorrect": true, "errorDescription": null },
          { "stepNumber": 3, "stepContent": "x = (14 - 16)/10 = -0.2", "isCorrect": false, "errorDescription": "Ildizlarni hisoblashda ishora xatosi bor." }
        ],
        "plagiarismScore": 12.0,
        "plagiarismType": "CLEAN",
        "plagiarismEvidence": "Yozuv uslubi va ba'zi sodda xatolar o'quvchining mustaqil ishlaganidan dalolat beradi."
      }""";

  @Test
  void parsesGradingResultFromOpenAiShapedResponse() throws Exception {
    var response =
        jackson3Mapper.readTree(
            wrapAsChatCompletion(GRADING_JSON.replace("\"", "\\\"").replace("\n", "\\n")));

    QwenGradingResult result = QwenAIClient.parseGradingResult(response, objectMapper);

    assertThat(result.criteriaScores()).hasSize(3);
    assertThat(result.criteriaScores().get(0))
        .isEqualTo(new CriteriaScore("Yechish usuli", 40, 90));
    assertThat(result.stepAnalyses()).hasSize(2);
    assertThat(result.stepAnalyses().get(1).errorDescription())
        .isEqualTo("Ildizlarni hisoblashda ishora xatosi bor.");
    assertThat(result.plagiarismType()).isEqualTo("CLEAN");
    assertThat(result.plagiarismScore()).isEqualTo(12.0);
  }

  @Test
  void stripsMarkdownCodeFenceBeforeParsing() throws Exception {
    String fenced = "```json\n" + GRADING_JSON + "\n```";
    var response =
        jackson3Mapper.readTree(
            wrapAsChatCompletion(fenced.replace("\"", "\\\"").replace("\n", "\\n")));

    QwenGradingResult result = QwenAIClient.parseGradingResult(response, objectMapper);

    assertThat(result.criteriaScores()).hasSize(3);
  }

  @Test
  void throwsQwenUnavailableExceptionOnUnparsableContent() throws Exception {
    var response = jackson3Mapper.readTree(wrapAsChatCompletion("not json at all"));

    assertThatThrownBy(() -> QwenAIClient.parseGradingResult(response, objectMapper))
        .isInstanceOf(QwenUnavailableException.class);
  }

  @Test
  void buildsUserContentWithSubjectCriteriaAndText() {
    String content =
        QwenAIClient.buildUserContent(
            "Matematika 7-sinf", List.of(new GradingCriterion("Yechish usuli", 40)), "5x + 3 = 18");

    assertThat(content)
        .contains("Matematika 7-sinf")
        .contains("Yechish usuli")
        .contains("40")
        .contains("5x + 3 = 18");
  }

  private static final String LESSON_PLAN_JSON =
      """
      {
        "objectives": ["Chiziqli tenglamalarni yechishni o'rganish"],
        "activities": [
          { "description": "Kirish va nazariy tushuntirish", "durationMinutes": 10 },
          { "description": "Doskada misollar yechish", "durationMinutes": 20 }
        ],
        "materials": ["Darslik", "Doska"],
        "homeworkSuggestion": "10-15 misollar"
      }""";

  @Test
  void parsesLessonPlanContentFromOpenAiShapedResponse() throws Exception {
    var response =
        jackson3Mapper.readTree(
            wrapAsChatCompletion(LESSON_PLAN_JSON.replace("\"", "\\\"").replace("\n", "\\n")));

    LessonPlanContent result = QwenAIClient.parseLessonPlanContent(response, objectMapper);

    assertThat(result.objectives()).containsExactly("Chiziqli tenglamalarni yechishni o'rganish");
    assertThat(result.activities()).hasSize(2);
    assertThat(result.activities().get(1).durationMinutes()).isEqualTo(20);
    assertThat(result.materials()).containsExactly("Darslik", "Doska");
    assertThat(result.homeworkSuggestion()).isEqualTo("10-15 misollar");
  }

  @Test
  void throwsQwenUnavailableExceptionOnUnparsableLessonPlanContent() throws Exception {
    var response = jackson3Mapper.readTree(wrapAsChatCompletion("not json at all"));

    assertThatThrownBy(() -> QwenAIClient.parseLessonPlanContent(response, objectMapper))
        .isInstanceOf(QwenUnavailableException.class);
  }

  private static String wrapAsChatCompletion(String escapedContent) {
    return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\""
        + escapedContent
        + "\"}}]}";
  }
}

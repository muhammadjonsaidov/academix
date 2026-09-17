package uz.academixai.school.application;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Guesses which detected spreadsheet column maps to which student field, from header text alone.
 *
 * <p>academix_tz.md §2.2 / the bulk-import-wizard skill call for this to be a Qwen call ({@code
 * qwenClient.suggestColumnMapping}) — no {@code QwenClient} exists in this codebase yet (that's the
 * separate {@code wire-ai-integration} scope). The skill's own rule 5 requires this suggestion step
 * to gracefully degrade to manual mapping if the AI call is unavailable, so this deterministic
 * synonym-matching heuristic *is* that degraded path — always taken today, not just on budget
 * exhaustion. Swap in a real Qwen call behind this same interface once wire-ai-integration lands;
 * the admin can always override the suggestion before commit either way, so this is a
 * correctness-neutral placeholder, not a functional gap.
 */
@Component
public class ColumnMappingSuggester {

  private static final Map<String, List<String>> SYNONYMS =
      Map.of(
          "firstName", List.of("ism", "name", "имя", "ismi"),
          "lastName", List.of("familiya", "familya", "surname", "фамилия"),
          "phone", List.of("telefon", "tel raqam", "tel", "phone", "телефон"),
          "classId", List.of("sinf", "klass", "class", "класс"),
          "studentNumber", List.of("o'quvchi raqami", "student number", "raqam", "id", "№"),
          "birthDate", List.of("tug'ilgan sana", "tugilgan sana", "sana", "birth", "дата"));

  // Declares match priority when a header could satisfy more than one field (e.g. "raqam" alone
  // is a substring hit for both phone and studentNumber) — more specific synonyms first.
  private static final List<String> FIELD_PRIORITY =
      List.of("phone", "birthDate", "firstName", "lastName", "classId", "studentNumber");

  public Map<String, String> suggest(List<String> detectedColumns) {
    Map<String, String> suggestion = new LinkedHashMap<>();
    for (String field : FIELD_PRIORITY) {
      for (String header : detectedColumns) {
        if (suggestion.containsValue(header)) {
          continue;
        }
        if (matches(field, header)) {
          suggestion.put(field, header);
          break;
        }
      }
    }
    return suggestion;
  }

  private boolean matches(String field, String header) {
    String normalized = normalize(header);
    return SYNONYMS.get(field).stream()
        .anyMatch(synonym -> normalized.contains(normalize(synonym)));
  }

  private String normalize(String value) {
    String stripped = Normalizer.normalize(value, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
    return stripped.toLowerCase().replace("'", "").replace("’", "").trim();
  }
}

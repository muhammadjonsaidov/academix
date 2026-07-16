package uz.academixai.infrastructure.ai;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import uz.academixai.domain.SubjectType;

/**
 * academix_tz.md §1.9 step 1's own worked example ("masalan MATH uchun formula solver").
 *
 * <p><b>MVP-level, deliberately narrow</b> (judgment call, see ROADMAP.md Sprint 3): only catches
 * an obviously-broken linear equation (a zero coefficient, e.g. AI-generated "0x + 5 = 10", which
 * has no solution) — not a general computer-algebra solver. Content that isn't a recognizable
 * {@code ax + b = c} shape is left to the independent AI verification step rather than guessed at
 * here; this validator's job per the spec is screening out *definitely* wrong generations cheaply,
 * not validating every possible math task shape.
 */
@Component
public class MathFormulaValidator implements UniqueTaskValidator {

  private static final Pattern LINEAR_EQUATION =
      Pattern.compile("(-?\\d*)x\\s*([+-]\\s*\\d+)?\\s*=\\s*(-?\\d+)");

  @Override
  public SubjectType subjectType() {
    return SubjectType.MATH;
  }

  @Override
  public boolean isSolvable(String taskContent) {
    if (taskContent == null || taskContent.isBlank()) {
      return false;
    }
    Matcher matcher = LINEAR_EQUATION.matcher(taskContent.replace(" ", ""));
    while (matcher.find()) {
      String coefficientRaw = matcher.group(1);
      int coefficient =
          coefficientRaw == null || coefficientRaw.isEmpty() || coefficientRaw.equals("-")
              ? (coefficientRaw != null && coefficientRaw.equals("-") ? -1 : 1)
              : Integer.parseInt(coefficientRaw);
      if (coefficient == 0) {
        return false;
      }
    }
    return true;
  }
}

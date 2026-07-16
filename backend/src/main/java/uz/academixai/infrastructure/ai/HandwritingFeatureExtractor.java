package uz.academixai.infrastructure.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * academix_tz.md §1.13/backend_tdd.md §6.2 — {@code HandwritingFeatureExtractor.extract(layout)},
 * returning {@code float[128]}. The spec names the inputs (slant, character spacing, relative
 * sizes) and explicitly allows "Java Math" (not necessarily an ML model) — this is a deterministic
 * geometric-histogram heuristic, judgment call (see ROADMAP.md Sprint 5), not a full ML model.
 *
 * <p><b>Why normalized, not raw pixels:</b> the same student's handwriting photographed at
 * different zoom/distance/resolution would otherwise produce wildly different raw pixel
 * measurements between two genuinely-matching submissions. Every metric is divided by the image's
 * own median character height, making the vector scale-invariant — a classic technique in
 * writer-identification literature (normalized geometric features), not novel here.
 *
 * <p>8 metrics × 16 histogram bins = 128 dimensions. Each metric's histogram is normalized to sum
 * to 1 (a proportion, not a raw count) so submissions with more or less text still produce
 * comparable vectors.
 */
@Component
public class HandwritingFeatureExtractor {

  private static final int BINS_PER_METRIC = 16;
  public static final int VECTOR_DIMENSIONS = 128;

  public float[] extract(DocumentTextLayout layout) {
    List<CharacterBox> chars = layout.characters();
    float[] vector = new float[VECTOR_DIMENSIONS];
    if (chars.isEmpty()) {
      return vector;
    }

    double medianHeight = median(chars.stream().map(CharacterBox::height).toList());
    if (medianHeight <= 0) {
      medianHeight = 1;
    }

    List<Double> widths = new ArrayList<>();
    List<Double> heights = new ArrayList<>();
    List<Double> aspectRatios = new ArrayList<>();
    List<Double> slants = new ArrayList<>();
    for (CharacterBox c : chars) {
      widths.add(c.width() / medianHeight);
      heights.add(c.height() / medianHeight);
      aspectRatios.add(c.height() == 0 ? 0 : c.width() / c.height());
      slants.add(c.slantRadians());
    }

    List<Double> intraWordGaps = new ArrayList<>();
    List<Double> interWordGaps = new ArrayList<>();
    for (int i = 0; i < chars.size() - 1; i++) {
      CharacterBox current = chars.get(i);
      double gap = current.horizontalGapTo(chars.get(i + 1)) / medianHeight;
      switch (current.breakAfter()) {
        case NONE -> intraWordGaps.add(gap);
        case SPACE -> interWordGaps.add(gap);
        case LINE_BREAK -> {
          // vertical, not horizontal — handled by line grouping below
        }
      }
    }

    List<Double> baselineDeviations = new ArrayList<>();
    List<Double> lineMedianBaselines = new ArrayList<>();
    List<CharacterBox> currentLine = new ArrayList<>();
    for (CharacterBox c : chars) {
      currentLine.add(c);
      if (c.breakAfter() == CharacterBox.BreakType.LINE_BREAK) {
        collectLine(currentLine, medianHeight, baselineDeviations, lineMedianBaselines);
        currentLine = new ArrayList<>();
      }
    }
    if (!currentLine.isEmpty()) {
      collectLine(currentLine, medianHeight, baselineDeviations, lineMedianBaselines);
    }
    List<Double> lineSpacings = new ArrayList<>();
    for (int i = 0; i < lineMedianBaselines.size() - 1; i++) {
      lineSpacings.add(
          (lineMedianBaselines.get(i + 1) - lineMedianBaselines.get(i)) / medianHeight);
    }

    int offset = 0;
    offset = fillHistogram(vector, offset, widths, 0, 3);
    offset = fillHistogram(vector, offset, heights, 0, 2);
    offset = fillHistogram(vector, offset, aspectRatios, 0, 3);
    offset = fillHistogram(vector, offset, intraWordGaps, -0.5, 2);
    offset = fillHistogram(vector, offset, interWordGaps, 0, 5);
    offset = fillHistogram(vector, offset, lineSpacings, 0.5, 4);
    offset = fillHistogram(vector, offset, slants, -1, 1);
    fillHistogram(vector, offset, baselineDeviations, -1, 1);
    return vector;
  }

  private static void collectLine(
      List<CharacterBox> line,
      double medianHeight,
      List<Double> baselineDeviations,
      List<Double> lineMedianBaselines) {
    List<Double> baselines = line.stream().map(CharacterBox::baselineY).toList();
    double medianBaseline = median(baselines);
    lineMedianBaselines.add(medianBaseline);
    for (double baseline : baselines) {
      baselineDeviations.add((baseline - medianBaseline) / medianHeight);
    }
  }

  private static double median(List<Double> values) {
    if (values.isEmpty()) {
      return 0;
    }
    List<Double> sorted = new ArrayList<>(values);
    Collections.sort(sorted);
    int mid = sorted.size() / 2;
    return sorted.size() % 2 == 0 ? (sorted.get(mid - 1) + sorted.get(mid)) / 2.0 : sorted.get(mid);
  }

  private static int fillHistogram(
      float[] vector, int offset, List<Double> values, double min, double max) {
    int[] counts = new int[BINS_PER_METRIC];
    for (double v : values) {
      double clamped = Math.max(min, Math.min(max, v));
      int bin = (int) ((clamped - min) / (max - min) * (BINS_PER_METRIC - 1));
      bin = Math.max(0, Math.min(BINS_PER_METRIC - 1, bin));
      counts[bin]++;
    }
    int total = values.size();
    for (int i = 0; i < BINS_PER_METRIC; i++) {
      vector[offset + i] = total == 0 ? 0f : (float) (counts[i] / (double) total);
    }
    return offset + BINS_PER_METRIC;
  }
}

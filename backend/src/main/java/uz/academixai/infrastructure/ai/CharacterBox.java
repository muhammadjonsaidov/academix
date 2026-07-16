package uz.academixai.infrastructure.ai;

import java.util.List;

/**
 * One Google Vision {@code symbol} (a single character) with its 4-vertex bounding box, plus what
 * break (if any) follows it — needed to regroup characters into words/lines for {@link
 * HandwritingFeatureExtractor}. academix_tz.md §3.1's response shape, drilled down to symbol level
 * (not modeled in the spec's own illustrative JSON, which stops at block level).
 */
public record CharacterBox(String text, List<double[]> vertices, BreakType breakAfter) {

  public enum BreakType {
    NONE,
    SPACE,
    LINE_BREAK
  }

  double minX() {
    return vertices.stream().mapToDouble(v -> v[0]).min().orElse(0);
  }

  double maxX() {
    return vertices.stream().mapToDouble(v -> v[0]).max().orElse(0);
  }

  /** Horizontal gap from this box's right edge to {@code next}'s left edge. */
  public double horizontalGapTo(CharacterBox next) {
    return next.minX() - maxX();
  }

  private double minY() {
    return vertices.stream().mapToDouble(v -> v[1]).min().orElse(0);
  }

  private double maxY() {
    return vertices.stream().mapToDouble(v -> v[1]).max().orElse(0);
  }

  public double width() {
    return maxX() - minX();
  }

  public double height() {
    return maxY() - minY();
  }

  public double centerX() {
    return (minX() + maxX()) / 2.0;
  }

  /** Bottom edge, used as a baseline proxy (top-left origin, y grows downward). */
  public double baselineY() {
    return maxY();
  }

  /**
   * Slant angle (radians) of the box's left edge from vertical — {@code vertices} is
   * top-left/top-right/bottom-right/bottom-left in Vision's own ordering.
   */
  public double slantRadians() {
    if (vertices.size() < 4) {
      return 0;
    }
    double[] topLeft = vertices.get(0);
    double[] bottomLeft = vertices.get(3);
    double dx = bottomLeft[0] - topLeft[0];
    double dy = bottomLeft[1] - topLeft[1];
    return dy == 0 ? 0 : Math.atan2(dx, dy);
  }
}

package uz.academixai.application;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Deterministic, overlap-preserving chunker for retrieval; it never splits a word when avoidable.
 */
@Component
public class SyllabusChunker {

  static final int MAX_CHUNK_CHARACTERS = 1_400;
  static final int OVERLAP_CHARACTERS = 250;
  static final int MINIMUM_SOURCE_CHARACTERS = 40;

  public List<String> chunk(String source) {
    String normalized = source == null ? "" : source.replaceAll("\\s+", " ").trim();
    if (normalized.length() < MINIMUM_SOURCE_CHARACTERS) {
      return List.of();
    }
    List<String> chunks = new ArrayList<>();
    int start = 0;
    while (start < normalized.length()) {
      int tentativeEnd = Math.min(normalized.length(), start + MAX_CHUNK_CHARACTERS);
      int end = breakAtSentenceOrSpace(normalized, start, tentativeEnd);
      String chunk = normalized.substring(start, end).trim();
      if (!chunk.isBlank()) {
        chunks.add(chunk);
      }
      if (end >= normalized.length()) {
        break;
      }
      start = Math.max(end - OVERLAP_CHARACTERS, start + 1);
      while (start < normalized.length() && Character.isWhitespace(normalized.charAt(start))) {
        start++;
      }
    }
    return chunks;
  }

  static int approximateTokenCount(String text) {
    return Math.max(1, (text.length() + 3) / 4);
  }

  private static int breakAtSentenceOrSpace(String text, int start, int tentativeEnd) {
    if (tentativeEnd == text.length()) {
      return tentativeEnd;
    }
    int lowerBound = start + (MAX_CHUNK_CHARACTERS * 2 / 3);
    for (int index = tentativeEnd; index > lowerBound; index--) {
      char current = text.charAt(index - 1);
      if (current == '.' || current == '!' || current == '?' || current == '\n') {
        return index;
      }
    }
    int space = text.lastIndexOf(' ', tentativeEnd);
    return space > lowerBound ? space : tentativeEnd;
  }
}

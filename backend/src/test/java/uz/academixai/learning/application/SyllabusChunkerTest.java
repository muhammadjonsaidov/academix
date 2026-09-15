package uz.academixai.learning.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SyllabusChunkerTest {

  private final SyllabusChunker chunker = new SyllabusChunker();

  @Test
  void preservesMeaningfulOverlapAcrossLongSource() {
    String source = ("Algebraik ifodalarni soddalashtirish mavzusi. ").repeat(90);

    List<String> chunks = chunker.chunk(source);

    assertThat(chunks).hasSizeGreaterThan(1);
    assertThat(chunks).allMatch(chunk -> chunk.length() <= SyllabusChunker.MAX_CHUNK_CHARACTERS);
    assertThat(chunks.get(1)).contains("Algebraik ifodalarni");
  }

  @Test
  void ignoresEmptyOrUnusableExtractedText() {
    assertThat(chunker.chunk("  \n  qisqa  ")).isEmpty();
  }
}

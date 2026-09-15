package uz.academixai.application;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Collectors;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import uz.academixai.domain.FileType;
import uz.academixai.infrastructure.ai.GoogleVisionClient;

/** Extracts source text locally where possible; OCR is used only for image uploads. */
@Component
public class SyllabusTextExtractor {

  private final GoogleVisionClient googleVisionClient;

  public SyllabusTextExtractor(GoogleVisionClient googleVisionClient) {
    this.googleVisionClient = googleVisionClient;
  }

  public String extract(FileType fileType, byte[] fileBytes) {
    try {
      return switch (fileType) {
        case PDF -> extractPdf(fileBytes);
        case DOCX -> extractDocx(fileBytes);
        case IMAGE -> googleVisionClient.extractText(fileBytes).extractedText();
      };
    } catch (IOException exception) {
      throw new UncheckedIOException("Darslik faylidan matn chiqarib bo'lmadi", exception);
    }
  }

  private static String extractPdf(byte[] fileBytes) throws IOException {
    try (PDDocument document = Loader.loadPDF(fileBytes)) {
      if (document.isEncrypted()) {
        throw new IOException("Parol bilan himoyalangan PDF qo'llab-quvvatlanmaydi");
      }
      PDFTextStripper stripper = new PDFTextStripper();
      stripper.setSortByPosition(true);
      return stripper.getText(document);
    }
  }

  private static String extractDocx(byte[] fileBytes) throws IOException {
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(fileBytes))) {
      return document.getParagraphs().stream()
          .map(paragraph -> paragraph.getText())
          .collect(Collectors.joining("\n"));
    }
  }
}

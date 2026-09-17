package uz.academixai.learning.application.port.out;

import uz.academixai.learning.domain.FileType;

/**
 * Extracts selectable text from an uploaded syllabus. Server-side by design: a syllabus is never
 * sent to a third party just to read its text (scanned PDFs use the OCR provider through
 * Intelligence).
 */
public interface SyllabusTextExtraction {

  String extract(FileType fileType, byte[] fileBytes);
}

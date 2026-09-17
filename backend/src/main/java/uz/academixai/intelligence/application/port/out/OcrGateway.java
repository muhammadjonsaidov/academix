package uz.academixai.intelligence.application.port.out;

import uz.academixai.intelligence.domain.OcrDocument;

/** OCR provider boundary. */
public interface OcrGateway {

  OcrDocument extract(byte[] image);
}

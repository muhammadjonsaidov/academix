package uz.academixai.intelligence.application.port.out;

/** OCR provider boundary: turns a submission image into its full extracted text. */
public interface OcrGateway {

  String extract(byte[] image);
}

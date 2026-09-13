package uz.academixai.learning.application.port.out;

/** Object-storage boundary for validated exam paper images. */
public interface ExamImageStorage {

  void store(String key, byte[] content, String contentType);
}

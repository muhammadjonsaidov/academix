package uz.academixai.learning.application.port.out;

/** Object-storage boundary for a validated homework image. */
public interface HomeworkImageStorage {

  void store(String key, byte[] content, String contentType);
}

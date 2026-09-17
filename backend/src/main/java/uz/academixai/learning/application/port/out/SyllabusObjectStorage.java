package uz.academixai.learning.application.port.out;

/** Object storage for syllabus files (SeaweedFS behind an S3 API). */
public interface SyllabusObjectStorage {

  void upload(String objectKey, byte[] content, String contentType);

  byte[] download(String objectKey);
}

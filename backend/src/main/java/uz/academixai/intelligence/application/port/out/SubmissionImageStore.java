package uz.academixai.intelligence.application.port.out;

/** Private object-storage read boundary for a submitted image. */
public interface SubmissionImageStore {

  byte[] download(String key);
}

package uz.academixai.learning.application.port.out;

import java.util.UUID;

/** Abuse-protection boundary for submission image uploads. */
public interface UploadQuota {

  void enforce(UUID userId);
}

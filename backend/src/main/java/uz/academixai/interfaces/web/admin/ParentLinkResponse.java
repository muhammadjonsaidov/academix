package uz.academixai.interfaces.web.admin;

import java.util.UUID;
import uz.academixai.domain.ParentStudentLink;

public record ParentLinkResponse(
    UUID id,
    UUID parentUserId,
    UUID studentUserId,
    String relation,
    boolean biometricConsentGiven) {

  public static ParentLinkResponse from(ParentStudentLink domain) {
    return new ParentLinkResponse(
        domain.id(),
        domain.parentUserId(),
        domain.studentUserId(),
        domain.relation().name(),
        domain.biometricConsentGiven());
  }
}

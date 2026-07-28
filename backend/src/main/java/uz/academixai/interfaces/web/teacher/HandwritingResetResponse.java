package uz.academixai.interfaces.web.teacher;

import uz.academixai.application.HandwritingService.ResetResult;

public record HandwritingResetResponse(String newProfileVersion, int resetCountThisQuarter) {

  public static HandwritingResetResponse from(ResetResult result) {
    return new HandwritingResetResponse(result.newProfileVersion(), result.resetCountThisQuarter());
  }
}

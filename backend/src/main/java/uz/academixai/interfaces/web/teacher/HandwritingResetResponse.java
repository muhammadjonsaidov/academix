package uz.academixai.interfaces.web.teacher;

import uz.academixai.application.HandwritingService.ResetResult;

public record HandwritingResetResponse(String newProfileVersion, int resetCountThisSemester) {

  public static HandwritingResetResponse from(ResetResult result) {
    return new HandwritingResetResponse(
        result.newProfileVersion(), result.resetCountThisSemester());
  }
}

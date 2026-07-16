package uz.academixai.interfaces.web.teacher;

import uz.academixai.domain.ResetReason;

/** academix_tz.md §2.2 "Yozuv profilini reset qilish" */
public record HandwritingResetRequest(ResetReason reason, String notes) {}

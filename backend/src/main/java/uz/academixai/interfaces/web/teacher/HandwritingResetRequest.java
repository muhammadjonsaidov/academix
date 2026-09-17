package uz.academixai.interfaces.web.teacher;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import uz.academixai.domain.ResetReason;

/** academix_tz.md §2.2 "Yozuv profilini reset qilish" */
public record HandwritingResetRequest(
    @NotNull(message = "majburiy maydon") ResetReason reason,
    @Size(max = 500, message = "ko'pi bilan 500 belgi") String notes) {}

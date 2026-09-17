package uz.academixai.interfaces.web.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import uz.academixai.family.domain.ParentRelation;

/** academix_tz.md — POST /admin/parents/link body, exact shape. */
public record LinkParentRequest(
    @NotBlank(message = "majburiy maydon") @Size(max = 20, message = "ko'pi bilan 20 belgi")
        String parentPhone,
    @NotNull(message = "majburiy maydon") UUID studentId,
    @NotNull(message = "majburiy maydon") ParentRelation relation) {}

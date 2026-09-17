package uz.academixai.interfaces.web.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import uz.academixai.family.domain.ParentRelation;

/** academix_tz.md — POST /admin/parents/link body, exact shape. */
public record LinkParentRequest(
    @NotBlank @Size(max = 20) String parentPhone,
    @NotNull UUID studentId,
    @NotNull ParentRelation relation) {}

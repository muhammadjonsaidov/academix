package uz.academixai.interfaces.web.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code type} must be a {@link uz.academixai.domain.SubjectType} name; icon is optional. */
public record CreateSubjectRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 30) String type,
    @Size(max = 50) String icon) {}

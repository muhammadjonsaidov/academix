package uz.academixai.interfaces.web.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** academix_tz.md §2.2 {@code PUT /admin/school} — exact body shape. */
public record UpdateSchoolRequest(
    @NotBlank @Size(max = 150) String name,
    @NotBlank @Size(max = 500) String address,
    @NotBlank @Size(max = 50) String region,
    @NotBlank @Size(max = 50) String district,
    @Size(max = 20) String phone) {}

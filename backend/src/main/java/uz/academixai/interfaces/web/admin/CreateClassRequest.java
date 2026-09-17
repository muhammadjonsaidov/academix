package uz.academixai.interfaces.web.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** academix_tz.md §2.2 — { grade: 7, letter: "A", classTeacherId: "uuid" } */
public record CreateClassRequest(
    @Min(1) @Max(11) int grade,
    @NotBlank @Size(max = 5) String letter,
    @NotNull UUID classTeacherId) {}

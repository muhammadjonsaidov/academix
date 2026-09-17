package uz.academixai.interfaces.web.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** academix_tz.md §2.2 — { grade: 7, letter: "A", classTeacherId: "uuid" } */
public record CreateClassRequest(
    @Min(value = 1, message = "kamida 1") @Max(value = 11, message = "ko'pi bilan 11") int grade,
    @NotBlank(message = "majburiy maydon") @Size(max = 5, message = "ko'pi bilan 5 belgi")
        String letter,
    @NotNull(message = "majburiy maydon") UUID classTeacherId) {}

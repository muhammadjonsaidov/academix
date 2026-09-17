package uz.academixai.interfaces.web.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/**
 * academix_tz.md §2.2 — { firstName, lastName, phone, classId, studentNumber, birthDate }.
 * DEVIATION on top: optional {@code email} and admin-chosen {@code password} — the admin creates
 * every account with its credentials directly (see StudentManagementService). Both may be null
 * (bulk import path keeps the server-generated temp password).
 */
public record CreateStudentRequest(
    @NotBlank(message = "majburiy maydon") @Size(max = 50, message = "ko'pi bilan 50 belgi")
        String firstName,
    @NotBlank(message = "majburiy maydon") @Size(max = 50, message = "ko'pi bilan 50 belgi")
        String lastName,
    @NotBlank(message = "majburiy maydon") @Size(max = 20, message = "ko'pi bilan 20 belgi")
        String phone,
    @Email(message = "email formati noto'g'ri") @Size(max = 100, message = "ko'pi bilan 100 belgi")
        String email,
    @NotBlank(message = "majburiy maydon") @Size(max = 72, message = "ko'pi bilan 72 belgi")
        String password,
    @NotNull(message = "majburiy maydon") UUID classId,
    @Size(max = 30, message = "ko'pi bilan 30 belgi") String studentNumber,
    @PastOrPresent(message = "kelajak sana bo'lmasligi kerak") LocalDate birthDate) {}

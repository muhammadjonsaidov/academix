package uz.academixai.interfaces.web.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code type} must be a {@link uz.academixai.domain.SubjectType} name; icon is optional. */
public record CreateSubjectRequest(
    @NotBlank(message = "majburiy maydon") @Size(max = 100, message = "ko'pi bilan 100 belgi")
        String name,
    @NotBlank(message = "majburiy maydon") @Size(max = 30, message = "ko'pi bilan 30 belgi")
        String type,
    @Size(max = 50, message = "ko'pi bilan 50 belgi") String icon) {}

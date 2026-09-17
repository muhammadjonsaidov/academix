package uz.academixai.interfaces.web.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** academix_tz.md §2.2 {@code PUT /admin/school} — exact body shape. */
public record UpdateSchoolRequest(
    @NotBlank(message = "majburiy maydon") @Size(max = 150, message = "ko'pi bilan 150 belgi")
        String name,
    @NotBlank(message = "majburiy maydon") @Size(max = 500, message = "ko'pi bilan 500 belgi")
        String address,
    @NotBlank(message = "majburiy maydon") @Size(max = 50, message = "ko'pi bilan 50 belgi")
        String region,
    @NotBlank(message = "majburiy maydon") @Size(max = 50, message = "ko'pi bilan 50 belgi")
        String district,
    @Size(max = 20, message = "ko'pi bilan 20 belgi") String phone) {}

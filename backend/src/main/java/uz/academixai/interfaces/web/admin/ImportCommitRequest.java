package uz.academixai.interfaces.web.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Map;

/** academix_tz.md §2.2 — { fileToken, columnMapping, saveMappingAsTemplate } */
public record ImportCommitRequest(
    @NotBlank(message = "majburiy maydon") @Size(max = 255, message = "ko'pi bilan 255 belgi")
        String fileToken,
    @NotEmpty(message = "bo'sh bo'lmasligi kerak") Map<String, String> columnMapping,
    boolean saveMappingAsTemplate) {}

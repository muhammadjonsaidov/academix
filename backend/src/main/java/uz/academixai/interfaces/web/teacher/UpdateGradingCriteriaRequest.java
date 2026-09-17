package uz.academixai.interfaces.web.teacher;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateGradingCriteriaRequest(
    @NotNull(message = "majburiy maydon") @NotEmpty(message = "bo'sh bo'lmasligi kerak") @Valid
        List<CriteriaItemDto> criteria) {}

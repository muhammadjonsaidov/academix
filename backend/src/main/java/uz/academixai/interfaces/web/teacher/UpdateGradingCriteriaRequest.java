package uz.academixai.interfaces.web.teacher;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateGradingCriteriaRequest(
    @NotNull @NotEmpty @Valid List<CriteriaItemDto> criteria) {}

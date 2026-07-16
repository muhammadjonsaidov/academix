package uz.academixai.interfaces.web.teacher;

import java.util.List;

public record UpdateGradingCriteriaRequest(List<CriteriaItemDto> criteria) {}

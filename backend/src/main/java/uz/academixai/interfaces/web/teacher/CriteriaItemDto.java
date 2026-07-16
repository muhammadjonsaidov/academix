package uz.academixai.interfaces.web.teacher;

import uz.academixai.domain.CriteriaItem;

public record CriteriaItemDto(String name, int weightPercent, String description) {

  public CriteriaItem toDomain() {
    return new CriteriaItem(name, weightPercent, description);
  }

  public static CriteriaItemDto from(CriteriaItem item) {
    return new CriteriaItemDto(item.name(), item.weightPercent(), item.description());
  }
}

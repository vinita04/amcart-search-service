package com.nagp.ecommerce.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Singular;
import org.springframework.util.CollectionUtils;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchDTO {

  @NotNull
  private String text;

  @Min(10)
  @Max(20)
  private int number = 15;
  private int from = 0;

  private String categoryPath;

  @Singular
  private List<String> brand;

  @Singular
  private List<String> color;

  @Singular
  private List<String> size;

  public Map<String, List<String>> getMapFilters() {
    return new HashMap<>() {{
      put("brand", CollectionUtils.isEmpty(brand) ? Collections.emptyList() : brand);
      put("color", CollectionUtils.isEmpty(color) ? Collections.emptyList() : color);
      put("size", CollectionUtils.isEmpty(size) ? Collections.emptyList() : size);
    }};
  }
}

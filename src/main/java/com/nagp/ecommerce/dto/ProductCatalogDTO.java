package com.nagp.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductCatalogDTO {
  private long number;
  private long total;
  private String suggestion;
  private List<ProductDTO> products;
}

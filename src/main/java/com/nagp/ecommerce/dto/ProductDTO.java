package com.nagp.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDTO {
  private int code;
  private String productId;
  private String productName;
  private String category;
  private String subcategory;
  private double price;
  private String description;
  private String categoryPath;
  private String brand;
  List<String> images;
  private String currency;
  List<String> size;
  private int stockQuantity;
  private String style;
  private String color;
  private String material;
  private Object facets;
}

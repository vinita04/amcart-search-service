package com.nagp.ecommerce.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product {

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
  private String material;
  private String color;
  private Object facets;
}

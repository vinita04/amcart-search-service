package com.nagp.ecommerce.mappers;

import com.nagp.ecommerce.dto.ProductDTO;
import com.nagp.ecommerce.models.Product;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {
  ProductDTO toDto(Product product);
}

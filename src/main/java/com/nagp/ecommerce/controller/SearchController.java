package com.nagp.ecommerce.controller;

import com.nagp.ecommerce.dto.*;
import com.nagp.ecommerce.service.SearchService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping(value = {"products"})
@Slf4j
@AllArgsConstructor
public class SearchController {

    private final SearchService service;

    @GetMapping("category/{category}")
    public ResponseEntity<ProductCatalogDTO> getProductsByCategory(@PathVariable("category") String category) throws IOException {
        return ResponseEntity.ok(service.getProductsByCategory(category));
    }
    @GetMapping("category/{category}/{subcategory}")
    public ResponseEntity<ProductCatalogDTO> getProductsByCategoryandSubcategory(@PathVariable("category") String category, @PathVariable("subcategory") String subcategory) throws IOException {
        return ResponseEntity.ok(service.getProductsByCategoryandSubcategory(category, subcategory));
    }

    @GetMapping("search")
    public ResponseEntity<ProductCatalogDTO> search(@Valid SearchDTO searchDTO) throws IOException {
        return ResponseEntity.ok(service.search(searchDTO));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDTO> searchByProductId(@PathVariable("productId") String productId) throws IOException {
        ProductDTO dto = service.searchByCode(productId);
        if (Objects.isNull(dto)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<Set<String>> getSuggestions(@Valid SearchDTO searchDTO) throws IOException {
        return ResponseEntity.ok(service.getSuggestions(searchDTO));
    }

    @GetMapping("/facets")
    public ResponseEntity<Map<String, List<FacetsDTO>>> getFacets(@Valid SearchDTO searchDTO) throws IOException {
        return ResponseEntity.ok(service.getFacets(searchDTO));
    }
}

package com.nagp.ecommerce.service;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.nagp.ecommerce.core.SearchCoreService;
import com.nagp.ecommerce.fields.FieldAttr;
import com.nagp.ecommerce.dto.*;
import com.nagp.ecommerce.mappers.ProductMapper;
import com.nagp.ecommerce.models.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import static com.nagp.ecommerce.fields.FieldAttr.Aggregations.*;

@Service
@RequiredArgsConstructor
public class SearchService {

  private final ProductMapper mapper;

  private final SearchCoreService coreService;

  public ProductCatalogDTO search(SearchDTO searchDTO) throws IOException {
    var response = coreService.searchTerm(searchDTO.getText(), searchDTO.getNumber(), searchDTO.getFrom(),
            searchDTO.getMapFilters(), searchDTO.getCategoryPath());
    var products = new ArrayList<ProductDTO>();
    getResultDocuments(response, products);
    return ProductCatalogDTO.builder()
        .products(products)
        .number(searchDTO.getNumber())
        .total(getTotalHits(response))
        .build();
  }

  private void getResultDocuments(SearchResponse<Product> response, ArrayList<ProductDTO> products) {
    for (var hit : response.hits().hits()) {
      var dto = mapper.toDto(hit.source());
      products.add(dto);
    }
  }

  public ProductCatalogDTO getProductsByCategory(String category) throws IOException {
    var response = coreService.getProductsByCategory(category);
    var products = new ArrayList<ProductDTO>();
    getResultDocuments(response, products);
    return ProductCatalogDTO.builder()
            .products(products)
            .total(getTotalHits(response))
            .build();
  }
  public ProductCatalogDTO getProductsByCategoryandSubcategory(String category, String subcategory) throws IOException {
    var response = coreService.getProductsByCategoryandSubcategory(category, subcategory);
    var products = new ArrayList<ProductDTO>();
    getResultDocuments(response, products);
    return ProductCatalogDTO.builder()
            .products(products)
            .total(getTotalHits(response))
            .build();
  }

  public Set<String> getSuggestions(SearchDTO searchDTO) throws IOException {
    var suggestionProducts = new HashSet<String>();
    var response = coreService.autocomplete(searchDTO.getText(), searchDTO.getNumber());
    var suggestions = response.suggest().get(FieldAttr.Suggest.PRODUCT_SUGGEST_NAME);
    for (var item : suggestions) {
      suggestionProducts.addAll(item.completion().options().stream().map(o -> o.source().getProductName()).collect(Collectors.toSet()));
    }
    return suggestionProducts;
  }

  public Map<String, List<FacetsDTO>> getFacets(SearchDTO searchDTO) throws IOException {
    var response = coreService.getFacets(searchDTO.getText(), searchDTO.getMapFilters(), searchDTO.getCategoryPath());
    return parseResults(response, List.of(FACET_COLOR_NAME, FACET_BRAND_NAME, FACET_SIZE_LIST));
  }

  private Map<String, List<FacetsDTO>> parseResults(SearchResponse<Void> response, List<String> aggNames) {
    Map<String, List<FacetsDTO>> facets = new HashMap<>();
    for (var name : aggNames) {
      var list = response.aggregations().get(name).sterms().buckets().array();
      var facetsList = list.stream().map(l -> new FacetsDTO(l.key(), l.docCount())).collect(Collectors.toList());
      facets.put(name.split("_")[1].toUpperCase(), facetsList);
    }
    return facets;
  }

  public ProductDTO searchByCode(String productId) throws IOException {
    SearchResponse<Product> response = coreService.getProductById(productId);
    if (response.hits().hits().size() == 0) {
      return null;
    }
    return mapper.toDto(response.hits().hits().get(0).source());
  }
  private long getTotalHits(SearchResponse<Product> response) {
    return Objects.nonNull(response.hits().total()) ? response.hits().total().value() : 0;
  }
}

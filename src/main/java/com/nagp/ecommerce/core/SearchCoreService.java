package com.nagp.ecommerce.core;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest.Builder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.*;
import com.nagp.ecommerce.fields.FieldAttr;
import com.nagp.ecommerce.dto.ProductSuggestDTO;
import com.nagp.ecommerce.models.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.http.util.TextUtils.isEmpty;

@Service
@RequiredArgsConstructor
public class SearchCoreService {

  @Value("${es.indexName}")
  private String index;
  private final ElasticsearchClient client;

  public SearchResponse<Product> searchTerm(String term, int number, int from, Map<String, List<String>> filters, String category) throws IOException {
    SearchRequest searchRequest = getSearchRequest(term, number, from, Collections.emptyMap(), filters, category);
    return client.search(searchRequest, Product.class);
  }

  public SearchResponse<Void> getFacets(String term, Map<String, List<String>> filters, String category) throws IOException {
    Map<String, Aggregation> map = new HashMap<>();
    map.put(FieldAttr.Aggregations.FACET_COLOR_NAME, new Aggregation.Builder()
        .terms(new TermsAggregation.Builder().field(FieldAttr.Aggregations.FACET_COLOR).size(25).build())
        .build());
    map.put(FieldAttr.Aggregations.FACET_BRAND_NAME, new Aggregation.Builder()
        .terms(new TermsAggregation.Builder().field(FieldAttr.Aggregations.FACET_BRAND).size(12).build())
        .build());
    map.put(FieldAttr.Aggregations.FACET_SIZE_LIST, new Aggregation.Builder()
        .terms(new TermsAggregation.Builder().field(FieldAttr.Aggregations.FACET_SIZE).size(12).build())
        .build());
    SearchRequest searchRequest = getSearchRequest(term, 0, 0, map, filters, category);
    return client.search(searchRequest, Void.class);
  }

  private SearchRequest getSearchRequest(String term, int number, int from,
      Map<String, Aggregation> map, Map<String, List<String>> filters, String category) {
        return SearchRequest.of(s -> {
          s.index(index);
          s.from(from);
          s.size(number);
          s.sort(sort -> sort.field(FieldSort.of(f -> f.field("code").order(SortOrder.Asc))));
          addQuery(s, term, filters, category);
          addAggregation(map, s);
          return s;
        });
  }

  private void addAggregation(Map<String, Aggregation> map, Builder s) {
    if (!map.isEmpty()) {
      s.aggregations(map);
    }
  }

  private void addSuggestion(Builder builder, String term) {
    Map<String, FieldSuggester> map = new HashMap<>();
    map.put(FieldAttr.Suggest.DID_YOU_MEAN, FieldSuggester.of(fs -> fs.phrase(p ->
            p.maxErrors(2.0).size(5).field(FieldAttr.Product.PRODUCT_SUGGEST)
        )
    ));
    Suggester suggester = Suggester.of(sg -> sg
        .suggesters("did_you_mean", new FieldSuggester.Builder()
            .phrase(PhraseSuggester.of(p ->
                p.maxErrors(2.0).size(5).field(FieldAttr.Product.PRODUCT_SUGGEST)))
            .build())
        .text(term)
    );
    builder.suggest(suggester);
  }

  public SearchResponse<ProductSuggestDTO> autocomplete(String term, int size) throws IOException {
    Map<String, FieldSuggester> map = new HashMap<>();
    map.put(FieldAttr.Suggest.PRODUCT_SUGGEST_NAME, FieldSuggester.of(fs -> fs
        .completion(cs -> cs.skipDuplicates(true)
            .size(size)
            .fuzzy(SuggestFuzziness.of(sf -> sf.fuzziness("1").transpositions(true).minLength(2).prefixLength(3)))
            .field(FieldAttr.Suggest.PRODUCT_SUGGEST)
        )
    ));
    Suggester suggester = Suggester.of(s -> s
        .suggesters(map)
        .text(term)
    );
    SearchRequest searchRequest = SearchRequest.of(s -> {
      s.index(index)
          .source(SourceConfig.of(sc -> sc.filter(f -> f.includes(List.of(FieldAttr.Product.PRODUCT_NAME)))))
          .suggest(suggester);
      return s;
    });
    return client.search(searchRequest, ProductSuggestDTO.class);
  }

  private void addQuery(Builder builder, String term, Map<String, List<String>> filters, String category) {
    if (isEmpty(category) && isEmpty(term)) {
      queryMatchAll(builder, filters);
    } else {
      buildBoolQuery(builder, term, filters, category);
    }
  }

  private void queryMatchAll(Builder builder, Map<String, List<String>> filters) {
    var filter = getFilters(filters);
    var matchAll = Query.of(q -> q.matchAll(MatchAllQuery.of(ma -> ma)));
    var boolQuery = BoolQuery.of(
        bq -> {
          if (filter.size() > 0) {
            bq.filter(filter);
          }
          bq.must(matchAll);
          return bq;
        }
    );
    builder.query(Query.of(q -> q.bool(boolQuery)));
  }

  private void buildBoolQuery(Builder builder, String term, Map<String, List<String>> mapFilters, String category) {
    var filters = getFilters(mapFilters);
    var matchQuery = Query
        .of(q -> q.match(MatchQuery.of(m -> m.field(FieldAttr.Product.PRODUCT_NAME).query(term).operator(Operator.And).boost(10f))));
    var multiMatchQuery = Query.of(q -> q.multiMatch(MultiMatchQuery.of(m ->
        m.fields(
            applyFieldBoost(FieldAttr.Product.PRODUCT_NAME, 5),
            applyFieldBoost(FieldAttr.Product.DESCRIPTION_FIELD, 2),
            applyFieldBoost(FieldAttr.Product.BRAND_FIELD, 2),
            applyFieldBoost(FieldAttr.Product.CATEGORY, 5))
            .operator(Operator.And).query(term))));
    var boolQuery = BoolQuery.of(
        bq -> {
          bq.filter(filters);
          if (!isEmpty(category)) {
            if (category.contains("/")) {
              var categoryList = category.split("/");
              bq.should(matchQuery, multiMatchQuery);
              bq.must(getMatchQuery(FieldAttr.Product.CATEGORY, categoryList[0]), getMatchQuery(FieldAttr.Product.SUBCATEGORY, categoryList[1]));
            } else {
              bq.should(matchQuery, multiMatchQuery);
              bq.must(getMatchQuery(FieldAttr.Product.CATEGORY, category));
            }
          } else {
            bq.should(matchQuery, multiMatchQuery);
          }
          //bq.minimumShouldMatch("1");
          return bq;
        }
    );
    builder.query(Query.of(q -> q.bool(boolQuery)));
  }

  private List<Query> getFilters(Map<String, List<String>> mapFilters) {
    var queries = new ArrayList<Query>();
    var brands = mapFilters.get("brand");
    if (!brands.isEmpty()) {
      var filters = brands.stream().map(g -> FieldValue.of(fv -> fv.stringValue(g))).collect(Collectors.toList());
      queries.add(getTermsQuery(filters, FieldAttr.Product.BRAND_FIELD));
    }

    var colors = mapFilters.get("color");
    if (!colors.isEmpty()) {
      var filters = colors.stream().map(g -> FieldValue.of(fv -> fv.stringValue(g))).collect(Collectors.toList());
      queries.add(getTermsQuery(filters, FieldAttr.Product.COLOR_FIELD));
    }

    var sizeList = mapFilters.get("size");
    if (!sizeList.isEmpty()) {
      var filters = sizeList.stream().map(g -> FieldValue.of(fv -> fv.stringValue(g))).collect(Collectors.toList());
      queries.add(getTermsQuery(filters, FieldAttr.Product.SIZE_FIELD));
    }
    return queries;
  }

  private Query getTermsQuery(List<FieldValue> filters, String field) {
    var termsQuery = Query.of(q ->
        q.terms(TermsQuery.of(tsq -> tsq.field(field)
                .terms(TermsQueryField.of(tf -> tf.value(filters))))));
    return termsQuery;
  }

  private String applyFieldBoost(String field, int boost) {
    return String.format("%s^%s", field, boost);
  }



  public SearchResponse<Product> getProductById(String productId) throws IOException {
    SearchRequest searchRequest = SearchRequest.of(s -> s.index(index)
        .size(12)
        .query(Query.of(q -> q.term(TermQuery.of(tq -> tq.value(productId).field("productId"))))));
    return client.search(searchRequest, Product.class);
  }

  public SearchResponse<Product> getProductsByCategory(String category) throws IOException {
    SearchRequest searchRequest = SearchRequest.of(s -> s.index(index)
        .size(12)
        .query(Query.of(q -> q.bool(BoolQuery.of(
          bq -> {
            bq.must(getMatchQuery(FieldAttr.Product.CATEGORY, category));
            return bq;
          })))));
    return client.search(searchRequest, Product.class);
  }
  public SearchResponse<Product> getProductsByCategoryandSubcategory(String category, String subcategory) throws IOException {
    SearchRequest searchRequest = SearchRequest.of(s -> s.index(index)
        .size(12)
        .query(Query.of(q -> q.bool(BoolQuery.of(
          bq -> {
            bq.must(getMatchQuery(FieldAttr.Product.CATEGORY, category), getMatchQuery(FieldAttr.Product.SUBCATEGORY, subcategory));
            return bq;
          })))));
    return client.search(searchRequest, Product.class);
  }

  private Query getMatchQuery(String field, String term) {
    return Query.of(q ->
            q.match(MatchQuery.of(m -> m.field(field).query(term).operator(Operator.And).boost(10f))));
  }
}

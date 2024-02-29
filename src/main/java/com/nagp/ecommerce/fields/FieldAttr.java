package com.nagp.ecommerce.fields;

public final class FieldAttr {

  private FieldAttr() {
  }

  public static class Product {

    private Product() {
    }

    public static final String PRODUCT_NAME = "productName";
    public static final String BRAND_FIELD = "brand.keyword";
    public static final String PRODUCT_SUGGEST = "product_suggest";
    public static final String DESCRIPTION_FIELD = "description";
    public static final String CATEGORY = "category";
    public static final String SUBCATEGORY = "subcategory";
    public static final String COLOR_FIELD = "facets.color.keyword";
    public static final String SIZE_FIELD = "facets.size.keyword";
  }

  public static class Suggest {

    private Suggest() {
    }

    public static final String PRODUCT_SUGGEST = "product_suggest";
    public static final String PRODUCT_SUGGEST_NAME = "product-suggest";
    public static final String DID_YOU_MEAN = "did_you_mean";
  }

  public static class Aggregations {

    private Aggregations() {
    }

    public static final String FACET_BRAND_NAME = "agg_brand";
    public static final String FACET_COLOR_NAME = "agg_color";
    public static final String FACET_SIZE_LIST = "agg_size";
    public static final String FACET_BRAND= "brand.keyword";
    public static final String FACET_COLOR = "facets.color.keyword";
    public static final String FACET_SIZE = "facets.size.keyword";
  }
}

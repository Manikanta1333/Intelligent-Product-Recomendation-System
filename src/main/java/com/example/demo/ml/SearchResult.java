package com.example.demo.ml;

import java.util.List;
import java.util.Map;

/**
 * Holds the result of a hybrid search query.
 * Now includes searchCategoryMap for per-category For You display.
 */
public class SearchResult {

    public final NLPSearchParser.ParsedQuery      parsedQuery;
    public final List<RankedProduct>              forYouProducts;    // flat list (backward compat)
    public final List<RankedProduct>              regularResults;
    public final Map<String, List<RankedProduct>> searchCategoryMap; // ✅ per-category For You

    public SearchResult(NLPSearchParser.ParsedQuery      parsedQuery,
                        List<RankedProduct>              forYouProducts,
                        List<RankedProduct>              regularResults,
                        Map<String, List<RankedProduct>> searchCategoryMap) {
        this.parsedQuery       = parsedQuery;
        this.forYouProducts    = forYouProducts;
        this.regularResults    = regularResults;
        this.searchCategoryMap = searchCategoryMap;
    }

    public boolean hasForYou() {
        return forYouProducts != null && !forYouProducts.isEmpty();
    }

    public boolean hasSearchCategoryMap() {
        return searchCategoryMap != null && !searchCategoryMap.isEmpty();
    }

    public int totalCount() {
        int fy = forYouProducts != null ? forYouProducts.size() : 0;
        int rr = regularResults != null ? regularResults.size() : 0;
        return fy + rr;
    }
}
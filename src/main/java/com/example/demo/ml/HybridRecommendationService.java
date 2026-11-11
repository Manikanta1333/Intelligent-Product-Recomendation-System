package com.example.demo.ml;

import com.example.demo.model.Order;
import com.example.demo.model.Product;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HybridRecommendationService {

    private static final int    MIN_ORDER_THRESHOLD  = 1;  // Any 1 order qualifies a category
    private static final int    FOR_YOU_PER_CATEGORY = 5;  // Top 5 per category in For You
    private static final int    SEARCH_FOR_YOU_COUNT = 5;  // Top 5 per category in search results
    private static final double MIN_RATING_GOOD      = 3.5;
    private static final double MIN_RATING_BEST      = 4.2;

    private final CollaborativeFilteringService collab;
    private final ContentBasedService           content;
    private final ProductRepository             productRepo;
    private final OrderRepository               orderRepo;
    private final NLPSearchParser               nlpParser;

    public HybridRecommendationService(
            CollaborativeFilteringService collab,
            ContentBasedService content,
            ProductRepository productRepo,
            OrderRepository orderRepo,
            NLPSearchParser nlpParser) {
        this.collab      = collab;
        this.content     = content;
        this.productRepo = productRepo;
        this.orderRepo   = orderRepo;
        this.nlpParser   = nlpParser;
    }

    // ============================================================
    // FOR-YOU: top-5 ML-ranked products per EVERY category the
    // user has interacted with, ordered by interaction frequency
    // ============================================================
    public Map<String, List<RankedProduct>> getForYouByCategory(Integer userId) {
        Map<String, List<RankedProduct>> result = new LinkedHashMap<>();
        if (userId == null) return result;

        List<Order> orders = orderRepo.findByUserIdOrderByOrderDateDesc(userId);
        if (orders == null || orders.isEmpty()) return result;

        // ✅ Collect all product IDs the user has already ordered
        Set<Integer> orderedProductIds = orders.stream()
                .map(Order::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // ✅ Count orders per category to rank categories by user preference
        Map<String, Long> categoryFrequency = new LinkedHashMap<>();
        for (Order o : orders) {
            if (o.getProductId() == null) continue;
            Product p = productRepo.findById(o.getProductId()).orElse(null);
            if (p == null || p.getMainCategory() == null) continue;
            categoryFrequency.merge(p.getMainCategory(), 1L, Long::sum);
        }

        // ✅ Include ALL categories the user ordered from (threshold = 1)
        // Sort by frequency desc so most-ordered categories appear first
        List<Map.Entry<String, Long>> qualifyingCats = categoryFrequency.entrySet().stream()
                .filter(e -> e.getValue() >= MIN_ORDER_THRESHOLD)
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toList());

        if (qualifyingCats.isEmpty()) return result;

        for (Map.Entry<String, Long> catEntry : qualifyingCats) {
            String category = catEntry.getKey();

            // ✅ Get all products in this category the user hasn't ordered yet
            List<Product> catProducts = productRepo.findByMainCategory(category)
                    .stream()
                    .filter(p -> !orderedProductIds.contains(p.getProductId()))
                    .collect(Collectors.toList());

            // ✅ If user has ordered every product in the category,
            //    still show top 5 (don't skip the category entirely)
            if (catProducts.isEmpty()) {
                catProducts = new ArrayList<>(productRepo.findByMainCategory(category));
            }

            if (catProducts.isEmpty()) continue;

            // ✅ Score every product in this category with the hybrid ML model
            //    (0.5 * collaborative score + 0.5 * content-based score)
            List<RankedProduct> top5 = rankProducts(catProducts, userId)
                    .stream()
                    .limit(FOR_YOU_PER_CATEGORY)
                    .collect(Collectors.toList());

            if (!top5.isEmpty()) {
                result.put(category, top5);
            }
        }

        return result; // LinkedHashMap preserves insertion order (most interacted → least)
    }

    // ============================================================
    // SEARCH: NLP parse → per-category top-5 For You sections
    // displayed above the regular ranked results list
    // ============================================================
    public SearchResult search(String rawQuery, Integer userId) {
        NLPSearchParser.ParsedQuery parsed = nlpParser.parse(rawQuery);
        List<Product> candidates = fetchSearchCandidates(parsed);

        if (candidates.isEmpty()) {
            return new SearchResult(parsed, List.of(), List.of(), new LinkedHashMap<>());
        }

        // ✅ Score and sort all candidates with hybrid model
        List<RankedProduct> ranked = rankProducts(candidates, userId);
        applyIntentModifiers(ranked, parsed);
        ranked.sort(Comparator.comparingDouble(RankedProduct::getHybridScore).reversed());

        if (userId != null && !parsed.resolvedCategories.isEmpty()) {
            // Check if the user has any orders before building personalised sections
            List<Order> userOrders = orderRepo.findByUserIdOrderByOrderDateDesc(userId);
            boolean hasOrders = userOrders != null && !userOrders.isEmpty();

            if (!hasOrders) {
                // Logged in but no order history — plain results, no For You
                return new SearchResult(parsed, List.of(), ranked, new LinkedHashMap<>());
            }

            // ✅ Build per-category top-5 "For You" map for search results
            // Each resolved search category gets its own top-5 row shown above regular results
            Map<String, List<RankedProduct>> searchCategoryMap = new LinkedHashMap<>();
            Set<Integer> addedToForYou = new HashSet<>();

            for (String cat : parsed.resolvedCategories) {
                List<RankedProduct> catTop5 = ranked.stream()
                        .filter(rp -> cat.equals(rp.getMainCategory()))
                        .limit(SEARCH_FOR_YOU_COUNT)
                        .collect(Collectors.toList());

                if (!catTop5.isEmpty()) {
                    searchCategoryMap.put(cat, catTop5);
                    catTop5.forEach(rp -> addedToForYou.add(rp.getProductId()));
                }
            }

            // Regular results = same resolved categories only, minus what's in ForYou.
            // This prevents unrelated products (shoes in "mobiles" search etc.)
            // from appearing below recommendations.
            Set<String> resolvedCatSet = new HashSet<>(parsed.resolvedCategories);
            List<RankedProduct> regularRest = ranked.stream()
                    .filter(rp -> resolvedCatSet.contains(rp.getMainCategory()))
                    .filter(rp -> !addedToForYou.contains(rp.getProductId()))
                    .collect(Collectors.toList());

            // Flat forYouProducts list for backward compat
            List<RankedProduct> forYouFlat = searchCategoryMap.values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            return new SearchResult(parsed, forYouFlat, regularRest, searchCategoryMap);

        } else {
            // Not logged in, or query matched no category → return ALL results as regular,
            // no "For You" section (guests get plain relevance-ranked results only)
            return new SearchResult(parsed, List.of(), ranked, new LinkedHashMap<>());
        }
    }

    // ============================================================
    // CORE RANKING: 0.4 * collaborative + 0.4 * content + personalisation
    // ============================================================
    private List<RankedProduct> rankProducts(List<Product> products, Integer userId) {
        UserPreferenceProfile profile = buildUserProfile(userId);
        List<RankedProduct> ranked = new ArrayList<>();
        for (Product p : products) {
            if (p == null) continue;
            double collScore   = collab.getCollaborativeScore(userId, p.getProductId());
            double contScore   = content.getContentScore(userId, p, 0.0);
            double hybridScore = 0.4 * collScore + 0.4 * contScore;
            hybridScore += profile.categoryBoost(p.getMainCategory());
            hybridScore += profile.brandBoost(p.getBrand());
            hybridScore += profile.priceRangeBoost(p.getSalePrice());
            ranked.add(new RankedProduct(p, collScore, contScore, hybridScore));
        }
        ranked.sort(Comparator.comparingDouble(RankedProduct::getHybridScore).reversed());
        return ranked;
    }

    private UserPreferenceProfile buildUserProfile(Integer userId) {
        if (userId == null) return UserPreferenceProfile.EMPTY;
        List<Order> orders = orderRepo.findByUserIdOrderByOrderDateDesc(userId);
        if (orders == null || orders.isEmpty()) return UserPreferenceProfile.EMPTY;

        Map<String, Long> catFreq   = new LinkedHashMap<>();
        Map<String, Long> brandFreq = new LinkedHashMap<>();
        List<Double>      prices    = new ArrayList<>();

        for (Order o : orders) {
            if (o.getProductId() == null) continue;
            Product p = productRepo.findById(o.getProductId()).orElse(null);
            if (p == null) continue;
            if (p.getMainCategory() != null) catFreq.merge(p.getMainCategory(), 1L, Long::sum);
            if (p.getBrand()        != null) brandFreq.merge(p.getBrand(),      1L, Long::sum);
            if (p.getSalePrice()    != null) prices.add(p.getSalePrice());
        }

        long maxCat   = catFreq.values().stream().mapToLong(v -> v).max().orElse(1);
        long maxBrand = brandFreq.values().stream().mapToLong(v -> v).max().orElse(1);
        Map<String, Double> catBoosts   = new LinkedHashMap<>();
        Map<String, Double> brandBoosts = new LinkedHashMap<>();
        catFreq.forEach((k, v)   -> catBoosts.put(k,   0.20 * v / (double) maxCat));
        brandFreq.forEach((k, v) -> brandBoosts.put(k, 0.15 * v / (double) maxBrand));

        double avgPrice = prices.stream().mapToDouble(d -> d).average().orElse(-1);
        double stdPrice = prices.size() > 1
            ? Math.sqrt(prices.stream().mapToDouble(d -> (d-avgPrice)*(d-avgPrice)).average().orElse(0))
            : Math.max(avgPrice * 0.5, 1.0);

        return new UserPreferenceProfile(catBoosts, brandBoosts, avgPrice, stdPrice);
    }

    private static class UserPreferenceProfile {
        static final UserPreferenceProfile EMPTY = new UserPreferenceProfile(Map.of(), Map.of(), -1, 0);
        private final Map<String, Double> catBoosts, brandBoosts;
        private final double avgPrice, stdPrice;

        UserPreferenceProfile(Map<String, Double> catBoosts, Map<String, Double> brandBoosts,
                              double avgPrice, double stdPrice) {
            this.catBoosts = catBoosts; this.brandBoosts = brandBoosts;
            this.avgPrice = avgPrice;   this.stdPrice = stdPrice;
        }
        double categoryBoost(String c) { return c == null ? 0 : catBoosts.getOrDefault(c, 0.0); }
        double brandBoost(String b)    { return b == null ? 0 : brandBoosts.getOrDefault(b, 0.0); }
        double priceRangeBoost(Double price) {
            if (avgPrice < 0 || price == null) return 0;
            double lo = avgPrice - 1.5 * stdPrice, hi = avgPrice + 1.5 * stdPrice;
            if (price >= lo && price <= hi) return 0.10;
            if (price < lo * 0.4 || price > hi * 2.5) return -0.05;
            return 0;
        }
    }

    // ============================================================
    // INTENT MODIFIERS: boost/penalise based on NLP price/quality signals
    //                   + strong boost when product name contains the keyword
    // ============================================================
    private void applyIntentModifiers(List<RankedProduct> ranked,
                                      NLPSearchParser.ParsedQuery parsed) {
        String kw = parsed.cleanKeyword != null ? parsed.cleanKeyword.toLowerCase().trim() : "";

        for (RankedProduct rp : ranked) {
            Product p = rp.getProduct();
            if (p == null) continue;
            double modifier = 1.0;

            // ── Keyword relevance boost ──────────────────────────────
            // Exact product name match → massive boost so it always tops results.
            // Partial name/brand match → strong boost.
            // No match within a resolved category → penalise (off-topic product).
            if (!kw.isBlank()) {
                String name  = p.getName()  != null ? p.getName().toLowerCase()  : "";
                String brand = p.getBrand() != null ? p.getBrand().toLowerCase() : "";
                if (name.equals(kw)) {
                    modifier += 0.80;   // exact full-name match → always first
                } else if (name.contains(kw) || brand.contains(kw)) {
                    modifier += 0.40;   // partial name/brand match
                } else if (!parsed.resolvedCategories.isEmpty()) {
                    modifier -= 0.35;   // in category but name doesn't match keyword
                }
            }

            // ── Price intent ─────────────────────────────────────────
            if (parsed.priceSignal == NLPSearchParser.PriceSignal.BUDGET) {
                double price = p.getSalePrice() != null ? p.getSalePrice() : 0;
                if (price < 500)        modifier += 0.15;
                else if (price < 2000)  modifier += 0.08;
                else if (price > 20000) modifier -= 0.10;
            } else if (parsed.priceSignal == NLPSearchParser.PriceSignal.PREMIUM) {
                double price = p.getSalePrice() != null ? p.getSalePrice() : 0;
                if (price > 20000)      modifier += 0.12;
                else if (price < 1000)  modifier -= 0.10;
            }

            // ── Rating intent ────────────────────────────────────────
            if (parsed.ratingSignal == NLPSearchParser.RatingSignal.BEST) {
                double r = p.getRatings() != null ? p.getRatings() : 0;
                modifier += (r >= MIN_RATING_BEST) ? 0.20 : -0.10;
            } else if (parsed.ratingSignal == NLPSearchParser.RatingSignal.GOOD) {
                double r = p.getRatings() != null ? p.getRatings() : 0;
                if (r >= MIN_RATING_GOOD) modifier += 0.10;
            }

            // ── Brand match ──────────────────────────────────────────
            if (parsed.hasBrand()) {
                String brand = p.getBrand() != null ? p.getBrand().toLowerCase() : "";
                for (String b : parsed.extractedBrands) {
                    if (brand.contains(b)) { modifier += 0.15; break; }
                }
            }

            rp.applyModifier(modifier);
        }
    }

    // ============================================================
    // SEARCH CANDIDATE FETCH
    // Priority:
    //   1. If NLP resolved categories → fetch ONLY those categories.
    //      Do NOT also run a LIKE search on top (avoids pulling 3000+
    //      unrelated products when cleanKeyword is blank after synonym stripping).
    //   2. If no category resolved → LIKE search on cleanKeyword.
    //   3. If cleanKeyword also blank → LIKE search on rawQuery.
    //   4. If still empty → return empty (no random fallback to all products).
    // ============================================================
    private List<Product> fetchSearchCandidates(NLPSearchParser.ParsedQuery parsed) {
        Set<Integer>  seen    = new LinkedHashSet<>();
        List<Product> results = new ArrayList<>();

        if (!parsed.resolvedCategories.isEmpty()) {
            // Category resolved — fetch ONLY those category products.
            // Never also run a LIKE search: after synonym stripping, cleanKeyword
            // may be a leftover fragment (e.g. "mobiles" → "s") that matches
            // thousands of completely unrelated products.
            // applyIntentModifiers boosts exact name-matches to the top within results.
            for (String cat : parsed.resolvedCategories) {
                for (Product p : productRepo.findByMainCategory(cat)) {
                    if (seen.add(p.getProductId())) results.add(p);
                }
            }
        } else {
            // No category resolved — rely purely on keyword LIKE search
            String kw = parsed.cleanKeyword;
            if (kw != null && !kw.isBlank()) {
                for (Product p : productRepo.findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase(kw, kw)) {
                    if (seen.add(p.getProductId())) results.add(p);
                }
            }
            // Fallback: try raw query only if keyword search also found nothing
            if (results.isEmpty() && parsed.rawQuery != null && !parsed.rawQuery.isBlank()) {
                String raw = parsed.rawQuery.trim().toLowerCase();
                for (Product p : productRepo.findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase(raw, raw)) {
                    if (seen.add(p.getProductId())) results.add(p);
                }
            }
        }

        return results;
    }
}
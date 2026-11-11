package com.example.demo.ml;

import com.example.demo.model.Product;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import smile.data.DataFrame;
import smile.regression.RandomForest;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * ============================================================
 * ContentBasedService
 * ============================================================
 * Loads content_ml.model (a RandomForest trained on 8 product
 * features) and provides per-product content scores.
 *
 * The model predicts an "interaction" score from:
 *   normCategory, normBrand, normPrice, normRating,
 *   normNumRatings, discountRatio, pricePerRating, ratingPopScore
 *
 * All normalization stats are carried inside ContentMLModel
 * (the serialized wrapper), so no separate properties file
 * is needed for the content model.
 *
 * getContentScore() returns a value in [0, 1].
 * ============================================================
 */
@Service
public class ContentBasedService {

    private static final Logger LOG =
            Logger.getLogger(ContentBasedService.class.getName());

    // Column names MUST match ContentMLTrainer
    private static final String[] COLS = {
            "normCategory", "normBrand", "normPrice", "normRating",
            "normNumRatings", "discountRatio", "pricePerRating",
            "ratingPopScore", "interaction"
    };

    private RandomForest         model;
    private Map<String, Integer> categoryMap;
    private Map<String, Integer> brandMap;
    private double               maxPrice;
    private double               maxRating;
    private double               maxNumRatings;
    private double               maxCatId;
    private double               maxBrandId;
    private boolean              ready = false;

    // Cache: productId → content score
    private final Map<Integer, Double> cache = new ConcurrentHashMap<>();

    // ── Startup ──────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    @PostConstruct
    public void init() {
        try {
            // ContentMLModel is the serialized wrapper from ContentMLTrainer
            try (ObjectInputStream ois = new ObjectInputStream(
                    new ClassPathResource("content_ml.model").getInputStream())) {

                // ContentMLModel is in the default package of the training project.
                // We deserialize it here as a plain Object and extract its fields
                // via reflection to avoid a hard compile-time dependency.
                Object obj = ois.readObject();

                // Use reflection to pull fields out of ContentMLModel
                java.lang.reflect.Field mf = obj.getClass().getField("model");
                java.lang.reflect.Field cf = obj.getClass().getField("categoryMap");
                java.lang.reflect.Field bf = obj.getClass().getField("brandMap");
                java.lang.reflect.Field pf = obj.getClass().getField("maxPrice");
                java.lang.reflect.Field rf = obj.getClass().getField("maxRating");
                java.lang.reflect.Field nf = obj.getClass().getField("maxNumRatings");

                model         = (RandomForest)         mf.get(obj);
                categoryMap   = (Map<String, Integer>) cf.get(obj);
                brandMap      = (Map<String, Integer>) bf.get(obj);
                maxPrice      = (double)               pf.get(obj);
                maxRating     = (double)               rf.get(obj);
                maxNumRatings = (double)               nf.get(obj);

                maxCatId   = Math.max(1, categoryMap.size());
                maxBrandId = Math.max(1, brandMap.size());
            }

            ready = true;
            LOG.info("ContentBasedService ready. categories=" + categoryMap.size() +
                     " brands=" + brandMap.size());

        } catch (Exception e) {
            LOG.severe("ContentBasedService init FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Returns a content-based score ∈ [0, 1] for a product.
     *
     * @param userId  ignored (content model is user-agnostic)
     * @param product the Product entity from DB
     * @param locationPopularity optional location signal [0,1]; pass 0 if unknown
     * @return score in [0, 1]
     */
    public double getContentScore(Integer userId, Product product, double locationPopularity) {
        if (!ready || product == null) return 0.5;

        return cache.computeIfAbsent(product.getProductId(), k -> {
            try {
                String category    = product.getMainCategory() != null ? product.getMainCategory() : "";
                String brand       = product.getBrand()        != null ? product.getBrand()        : "";
                double actualPrice  = product.getActualPrice()  != null ? product.getActualPrice()  : 0.0;
                double salePrice    = product.getSalePrice()    != null ? product.getSalePrice()    : actualPrice;
                double rating       = product.getRatings()      != null ? product.getRatings()      : 0.0;
                double numRatings   = product.getNoOfRatings()  != null ? product.getNoOfRatings()  : 0.0;

                double priceForNorm   = (salePrice > 0) ? salePrice : actualPrice;
                double normCategory   = categoryMap.getOrDefault(category, 0) / maxCatId;
                double normBrand      = brandMap.getOrDefault(brand, 0)       / maxBrandId;
                double normPrice      = (maxPrice   > 0) ? priceForNorm / maxPrice      : 0.0;
                double normRating     = (maxRating  > 0) ? rating       / maxRating     : 0.0;
                double normNumRatings = (maxNumRatings > 0)
                        ? Math.log1p(numRatings) / Math.log1p(maxNumRatings) : 0.0;
                double discountRatio  = (actualPrice > 0 && salePrice < actualPrice)
                        ? (actualPrice - salePrice) / actualPrice : 0.0;
                double pricePerRating = normPrice / (normRating + 1e-6);
                double ratingPopScore = normRating * normNumRatings;

                double[][] data = new double[][]{{
                        normCategory, normBrand, normPrice, normRating,
                        normNumRatings, discountRatio, pricePerRating, ratingPopScore,
                        0.0  // placeholder for label column
                }};

                DataFrame df  = DataFrame.of(data, COLS);
                double    raw = model.predict(df.get(0));

                // Soft-clamp via sigmoid so out-of-range values map to [0,1]
                return sigmoid(raw);

            } catch (Exception e) {
                return 0.5;
            }
        });
    }

    /** Convenience: score from product ID only (uses DB-less defaults). */
    public double getContentScoreById(Integer productId) {
        if (cache.containsKey(productId)) return cache.get(productId);
        return 0.5; // unknown product
    }

    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    public boolean isReady() { return ready; }
}
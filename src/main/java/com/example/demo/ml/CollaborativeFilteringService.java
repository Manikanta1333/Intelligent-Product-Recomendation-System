package com.example.demo.ml;

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
 * CollaborativeFilteringService
 * ============================================================
 * Loads the pre-trained collaborative.model (RandomForest)
 * from src/main/resources/ at startup.
 *
 * The model was trained with 3 features (normalized):
 *   [0] userId    / maxUserId
 *   [1] productId / maxProductId
 *   [2] region    (first 3 digits of pincode) / maxRegion
 *
 * Normalization stats are loaded from norm_stats.properties
 * which is saved alongside the model by SmileTrainer.
 *
 * predict() returns a score in roughly [0, 1] after min-max
 * clamping — used as the collaborative component in the hybrid.
 * ============================================================
 */
@Service
public class CollaborativeFilteringService {

    private static final Logger LOG =
            Logger.getLogger(CollaborativeFilteringService.class.getName());

    // Column names MUST match those used in SmileTrainer
    private static final String[] COLS =
            {"userId", "productId", "region", "interaction"};

    private RandomForest model;
    private boolean      ready = false;

    // Normalization stats (loaded from norm_stats.properties)
    private double maxUserId    = 1;
    private double maxProductId = 1;
    private double maxRegion    = 1;

    // Min/max of raw model output — calibrated lazily for score normalization
    private double outputMin =  Double.MAX_VALUE;
    private double outputMax = -Double.MAX_VALUE;

    // Small cache: (userId, productId) → score
    private final Map<String, Double> cache = new ConcurrentHashMap<>();

    // ── Startup ──────────────────────────────────────────────────
    @PostConstruct
    public void init() {
        try {
            // Load model
            try (ObjectInputStream ois = new ObjectInputStream(
                    new ClassPathResource("collaborative.model").getInputStream())) {
                model = (RandomForest) ois.readObject();
            }

            // Load normalization stats
            java.util.Properties p = new java.util.Properties();
            try (InputStream is =
                    new ClassPathResource("norm_stats.properties").getInputStream()) {
                p.load(is);
            }
            maxUserId    = Double.parseDouble(p.getProperty("maxUserId",    "1"));
            maxProductId = Double.parseDouble(p.getProperty("maxProductId", "1"));
            maxRegion    = Double.parseDouble(p.getProperty("maxRegion",    "1"));

            ready = true;
            LOG.info("CollaborativeFilteringService ready. " +
                     "maxUserId=" + maxUserId +
                     " maxProductId=" + maxProductId +
                     " maxRegion=" + maxRegion);

        } catch (Exception e) {
            LOG.severe("CollaborativeFilteringService init FAILED: " + e.getMessage());
        }
    }

    /**
     * Returns a collaborative score ∈ [0, 1] for (userId, productId).
     *
     * @param userId    session user id (null → 0.5 neutral)
     * @param productId product id from DB
     * @return score in [0, 1]
     */
    public double getCollaborativeScore(Integer userId, int productId) {
        if (!ready || userId == null) return 0.5;

        String key = userId + ":" + productId;
        return cache.computeIfAbsent(key, k -> {
            try {
                double normUserId    = Math.min((double) userId    / maxUserId,    1.0);
                double normProductId = Math.min((double) productId / maxProductId, 1.0);
                // region = 0 when we have no pincode — neutral mid-value
                double normRegion    = 0.5;

                double[][] data = new double[][]{{
                        normUserId, normProductId, normRegion, 0.0
                }};
                DataFrame df = DataFrame.of(data, COLS);
                double raw  = model.predict(df.get(0));

                // Lazy output-range calibration for normalization
                synchronized (this) {
                    if (raw < outputMin) outputMin = raw;
                    if (raw > outputMax) outputMax = raw;
                }
                return clamp(raw);

            } catch (Exception e) {
                return 0.5;
            }
        });
    }

    /**
     * Score with known pincode (more accurate).
     */
    public double getCollaborativeScoreWithPincode(Integer userId, int productId, String pincode) {
        if (!ready || userId == null) return 0.5;
        try {
            double normUserId    = Math.min((double) userId    / maxUserId,    1.0);
            double normProductId = Math.min((double) productId / maxProductId, 1.0);
            double region        = extractRegion(pincode);
            double normRegion    = (maxRegion > 0) ? Math.min(region / maxRegion, 1.0) : 0.5;

            double[][] data = new double[][]{{
                    normUserId, normProductId, normRegion, 0.0
            }};
            DataFrame df = DataFrame.of(data, COLS);
            double raw  = model.predict(df.get(0));
            return clamp(raw);

        } catch (Exception e) {
            return 0.5;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────

    /** Clamp raw model output to [0, 1] via sigmoid-like soft clamp. */
    private double clamp(double raw) {
        // Use sigmoid so extreme values map gracefully to 0/1
        return 1.0 / (1.0 + Math.exp(-raw * 0.5));
    }

    private double extractRegion(String pincode) {
        if (pincode == null) return 0;
        String s = pincode.trim().replaceAll("[^0-9]", "");
        try {
            if (s.length() >= 3) return Double.parseDouble(s.substring(0, 3));
            return Double.parseDouble(s);
        } catch (Exception e) { return 0; }
    }

    public boolean isReady() { return ready; }
}
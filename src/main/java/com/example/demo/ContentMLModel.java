package com.example.demo;

import smile.regression.RandomForest;

import java.io.Serializable;
import java.util.Map;

/**
 * Serialized wrapper produced by ContentMLTrainer.
 *
 * Must live in com.example.demo so that Java's ObjectInputStream
 * can resolve the class name stored inside content_ml.model.
 *
 * Place this file at:
 *   src/main/java/com/example/demo/ContentMLModel.java
 */
public class ContentMLModel implements Serializable {

    private static final long serialVersionUID = -3909478481996955728L;

    /** The trained RandomForest model. */
    public RandomForest model;

    /** Maps category name → integer id used during training. */
    public Map<String, Integer> categoryMap;

    /** Maps brand name → integer id used during training. */
    public Map<String, Integer> brandMap;

    /** Max sale/actual price seen during training (for normalization). */
    public double maxPrice;

    /** Max rating value seen during training (typically 5.0). */
    public double maxRating;

    /** Max number-of-ratings seen during training (for log normalization). */
    public double maxNumRatings;

    public ContentMLModel() {}

    public ContentMLModel(RandomForest model,
                          Map<String, Integer> categoryMap,
                          Map<String, Integer> brandMap,
                          double maxPrice,
                          double maxRating,
                          double maxNumRatings) {
        this.model         = model;
        this.categoryMap   = categoryMap;
        this.brandMap      = brandMap;
        this.maxPrice      = maxPrice;
        this.maxRating     = maxRating;
        this.maxNumRatings = maxNumRatings;
    }
}
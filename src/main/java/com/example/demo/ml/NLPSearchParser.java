package com.example.demo.ml;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * ============================================================
 * NLPSearchParser
 * ============================================================
 * Lightweight NLP layer that converts a free-text search query
 * into a set of structured signals:
 *
 *   - resolvedCategories  : matched DB category strings
 *   - extractedBrands     : brand names found in query
 *   - priceSignal         : budget / premium intent
 *   - ratingSignal        : quality intent (e.g. "best", "top rated")
 *   - cleanKeyword        : stripped keyword for DB LIKE search
 *
 * Works entirely offline — no external API needed.
 * ============================================================
 */
@Component
public class NLPSearchParser {

    public enum PriceSignal   { BUDGET, NEUTRAL, PREMIUM }
    public enum RatingSignal  { ANY, GOOD, BEST }

    // ── All 27 DB categories with NLP synonyms ──────────────────
    // Rules:
    //   1. Longer/more-specific synonyms listed FIRST within each entry
    //   2. Bare item-type words (dress, kurti, heels…) go here BUT are also
    //      preserved as cleanKeyword via PRESERVE_AS_KEYWORD so the LIKE
    //      search filters candidates to only matching products within the category.
    //   3. Avoid cross-category ambiguity: "heels"/"sandals" live ONLY in their
    //      own shoe categories, NOT in Womens Fashion.
    private static final List<CategoryEntry> CATEGORY_SYNONYMS = List.of(
        cat("Phones",
                "mobile phone","smartphone","cell phone","android phone","iphone",
                "phone","phones","mobile","mobiles","handset",
                "redmi","realme","oneplus","samsung phone"),
        cat("Televisions",
                "smart tv","led tv","oled tv","qled tv",
                "television","tv","monitor","display"),
        cat("Headphones",
                "bluetooth earphone","wireless earphone","bluetooth headphone","wireless headphone",
                "headphone","earphone","earbud","earbuds","airpod","neckband","headset","tws"),
        cat("Speakers",
                "bluetooth speaker","sound bar","soundbar","home theatre","subwoofer","speaker"),
        cat("Cameras",
                "action camera","dslr camera","mirrorless camera",
                "camera","dslr","mirrorless","webcam","gopro","photography"),
        cat("Refrigerators",
                "refrigerator","fridge","freezer"),
        // Womens Fashion — clothing ONLY. Shoes/sandals/innerwear are separate categories.
        cat("Womens Fashion",
                "women casual dress","women party dress","women maxi dress","women midi dress",
                "women ethnic wear","women western wear","women kurta","women kurti",
                "ladies dress","womens dress","girl dress","women dress",
                "women clothing","ladies clothing","womens fashion","women fashion",
                "women top","ladies top","women suit","women co-ord",
                "saree","lehenga","kurti","kurta","anarkali","salwar",
                "dress","gown","frock","maxi dress","midi dress","bodycon","jumpsuit"),
        // Shoes — keep heels/stiletto/pumps HERE, not in Womens Fashion
        cat("Womens Shoes",
                "women high heels","ladies high heels","women block heels","women platform heels",
                "ladies stiletto","women pumps","women formal shoes","women casual shoes",
                "womens shoes","women shoes","ladies shoes","women footwear","ladies footwear",
                "heels","stiletto","pumps","kitten heels"),
        cat("Womens Sandals",
                "womens sandals","women sandals","ladies sandals","women flats","ladies flats",
                "chappal women","slipper women","women chappal","women slippers","sandals"),
        cat("Womens Watches",
                "womens watch","women watch","ladies watch","girl watch"),
        cat("Womens Innerwear",
                "women innerwear","ladies innerwear","women lingerie","women bra",
                "bra","lingerie","panty","women undergarment","ladies undergarment"),
        cat("Handbags and Clutches",
                "ladies handbag","women handbag","shoulder bag","ladies bag","women bag",
                "handbag","clutch bag","clutch","purse","tote"),
        cat("Make-up",
                "lipstick","foundation","mascara","eyeshadow","eyeliner","bb cream","concealer",
                "makeup","make up","blush","cosmetic","beauty","face powder","kajal"),
        cat("Mens Shirts",
                "men formal shirt","mens formal shirt","men casual shirt","mens casual shirt",
                "men shirt","mens shirt","formal shirt","casual shirt","shirt"),
        cat("T-shirts and Polos",
                "men t-shirt","mens t-shirt","graphic tee","plain tee",
                "tshirt","t-shirt","polo","round neck","half sleeve"),
        cat("Mens Formal Shoes",
                "men formal shoes","mens formal shoes","men leather shoes","men oxford",
                "formal shoes","oxford","derby","office shoes","leather shoes"),
        cat("Mens Sports Shoes",
                "men running shoes","men sport shoes","mens sport shoes","men sneakers","men joggers",
                "sport shoes","running shoes","sneakers","gym shoes","joggers","training shoes"),
        cat("Mens Watches",
                "men smart watch","mens smart watch","mens watch","men watch",
                "smartwatch","smart watch","watch"),
        cat("Mens Innerwear",
                "men innerwear","mens innerwear","men underwear",
                "underwear","briefs","trunks","boxer"),
        cat("Kids Clothing",
                "kids clothes","children clothing","boy clothes","girl clothes",
                "kids wear","children wear","kids dress","boys wear","girls wear"),
        cat("Baby Products",
                "baby product","baby care","baby food","baby clothes",
                "diaper","nappy","infant","toddler","newborn"),
        cat("School Bags",
                "laptop bag","school bag","kids bag","college bag","travel bag",
                "backpack","rucksack","bag"),
        cat("Toys",
                "board game","action figure","remote control toy",
                "toy","game","lego","puzzle","doll"),
        cat("Strength Training",
                "gym equipment","weight training","home gym",
                "dumbbell","barbell","gym","fitness","exercise","workout","protein"),
        cat("Motorbike Accessories",
                "bike accessory","bike gear","riding gear",
                "helmet","motorcycle","motorbike"),
        cat("Musical Instruments",
                "musical instrument","music instrument",
                "guitar","piano","keyboard","drum","violin","ukulele"),
        cat("Home Improvement",
                "home improvement","power tool",
                "drill","plumbing","electrical","hardware","ladder","paint","tool")
    );

    // ── Keywords that must be PRESERVED as cleanKeyword even after category resolution ──
    // When a user types "dress", it resolves to Womens Fashion (good), but we still
    // need to LIKE-search "dress" within those candidates so heels/innerwear/etc.
    // that have no "dress" in their name get filtered out or ranked lower.
    private static final Set<String> PRESERVE_AS_KEYWORD = Set.of(
        // Clothing types
        "dress","gown","frock","maxi","midi","bodycon","jumpsuit","saree","lehenga",
        "kurti","kurta","anarkali","salwar","top","blouse","suit","co-ord",
        // Footwear types
        "heels","sandals","sneakers","loafers","boots","pumps","stiletto","flats","chappal",
        // Other specific item types
        "shirt","polo","tshirt","watch","smartwatch",
        "headphone","earphone","earbud","speaker","neckband",
        "dumbbell","barbell","protein","backpack","lipstick","foundation","mascara"
    );

    // ── Known brands (lower-case) ────────────────────────────────
    private static final Set<String> KNOWN_BRANDS = Set.of(
        "samsung","apple","oneplus","realme","redmi","xiaomi","oppo","vivo","nokia","motorola",
        "dell","hp","lenovo","asus","acer","msi","sony","lg","panasonic","philips","bosch",
        "nike","adidas","puma","reebok","skechers","bata","woodland","campus",
        "titan","fastrack","casio","fossil","timex","seiko",
        "boat","jbl","bose","sennheiser","skullcandy","oneodio",
        "nikon","canon","gopro","fujifilm",
        "levi","zara","h&m","allen solly","peter england","arrow",
        "lakme","maybeline","loreal","nykaa","mac","revlon","biotique",
        "hasbro","lego","mattel","funskool","fisher price",
        "decathlon","cosco","nivia","sg","ss","vector x"
    );

    // ── Price / quality intent keywords ─────────────────────────
    private static final Map<String, PriceSignal> PRICE_KEYWORDS = Map.of(
        "cheap", PriceSignal.BUDGET, "budget", PriceSignal.BUDGET,
        "affordable", PriceSignal.BUDGET, "low price", PriceSignal.BUDGET,
        "inexpensive", PriceSignal.BUDGET, "economy", PriceSignal.BUDGET,
        "premium", PriceSignal.PREMIUM, "luxury", PriceSignal.PREMIUM,
        "expensive", PriceSignal.PREMIUM, "high end", PriceSignal.PREMIUM
    );

    private static final Map<String, RatingSignal> QUALITY_KEYWORDS = Map.of(
        "best", RatingSignal.BEST, "top rated", RatingSignal.BEST,
        "top pick", RatingSignal.BEST, "highly rated", RatingSignal.BEST,
        "good", RatingSignal.GOOD, "recommended", RatingSignal.GOOD,
        "popular", RatingSignal.GOOD, "trending", RatingSignal.GOOD
    );

    // ============================================================
    // MAIN PARSE METHOD
    // ============================================================
    public ParsedQuery parse(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return new ParsedQuery(rawQuery, List.of(), List.of(),
                    PriceSignal.NEUTRAL, RatingSignal.ANY, rawQuery);
        }

        String q        = rawQuery.trim().toLowerCase();
        String qClean   = q.replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();

        // 1. Price intent
        PriceSignal priceSignal = PriceSignal.NEUTRAL;
        for (Map.Entry<String, PriceSignal> e : PRICE_KEYWORDS.entrySet()) {
            if (qClean.contains(e.getKey())) { priceSignal = e.getValue(); break; }
        }

        // 2. Rating intent
        RatingSignal ratingSignal = RatingSignal.ANY;
        for (Map.Entry<String, RatingSignal> e : QUALITY_KEYWORDS.entrySet()) {
            if (qClean.contains(e.getKey())) { ratingSignal = e.getValue(); break; }
        }

        // 3. Category matching — collect matched synonyms for stripping
        List<String> categories = new ArrayList<>();
        List<String> matchedSynonyms = new ArrayList<>();
        for (CategoryEntry ce : CATEGORY_SYNONYMS) {
            for (String syn : ce.synonyms) {
                if (qClean.contains(syn)) {
                    if (!categories.contains(ce.dbCategory))
                        categories.add(ce.dbCategory);
                    matchedSynonyms.add(syn);  // track which synonym matched
                    break;
                }
            }
        }

        // 4. Brand matching
        List<String> brands = new ArrayList<>();
        for (String brand : KNOWN_BRANDS) {
            if (qClean.contains(brand)) brands.add(brand);
        }

        // 5. Clean keyword: strip intent/modifier words AND matched category synonyms,
        //    BUT preserve specific item-type words (dress, kurti, heels, etc.) so
        //    the LIKE search filters candidates to the right product type.
        String cleanKw = stripIntentWords(qClean, matchedSynonyms);

        // If cleanKw is blank after stripping, check if any PRESERVE_AS_KEYWORD
        // word appears in the original query and restore it.
        if (cleanKw.isBlank()) {
            for (String kw : PRESERVE_AS_KEYWORD) {
                if (qClean.contains(kw)) { cleanKw = kw; break; }
            }
        }

        return new ParsedQuery(rawQuery, categories, brands, priceSignal, ratingSignal, cleanKw);
    }

    // ── Helpers ──────────────────────────────────────────────────

    private String stripIntentWords(String q, List<String> matchedCategorySynonyms) {
        String stripped = q;
        // Strip price/quality intent modifiers
        for (String kw : PRICE_KEYWORDS.keySet())   stripped = stripped.replace(kw, " ");
        for (String kw : QUALITY_KEYWORDS.keySet())  stripped = stripped.replace(kw, " ");
        // ✅ Strip matched category synonyms so "mobiles" / "phone" don't pollute LIKE search
        // Only strip if the query resolves to a category — avoids wiping brand+category combos
        for (String syn : matchedCategorySynonyms)  stripped = stripped.replace(syn, " ");
        return stripped.replaceAll("\\s+", " ").trim();
    }

    private static CategoryEntry cat(String db, String... synonyms) {
        return new CategoryEntry(db, synonyms);
    }

    // ── Inner types ──────────────────────────────────────────────

    private static class CategoryEntry {
        final String   dbCategory;
        final String[] synonyms;
        CategoryEntry(String db, String... syns) { dbCategory = db; synonyms = syns; }
    }

    public static class ParsedQuery {
        public final String       rawQuery;
        public final List<String> resolvedCategories;
        public final List<String> extractedBrands;
        public final PriceSignal  priceSignal;
        public final RatingSignal ratingSignal;
        public final String       cleanKeyword;

        public ParsedQuery(String rawQuery,
                           List<String> resolvedCategories,
                           List<String> extractedBrands,
                           PriceSignal priceSignal,
                           RatingSignal ratingSignal,
                           String cleanKeyword) {
            this.rawQuery           = rawQuery;
            this.resolvedCategories = resolvedCategories;
            this.extractedBrands    = extractedBrands;
            this.priceSignal        = priceSignal;
            this.ratingSignal       = ratingSignal;
            this.cleanKeyword       = cleanKeyword;
        }

        public boolean hasCategory() { return !resolvedCategories.isEmpty(); }
        public boolean hasBrand()    { return !extractedBrands.isEmpty(); }
    }
}
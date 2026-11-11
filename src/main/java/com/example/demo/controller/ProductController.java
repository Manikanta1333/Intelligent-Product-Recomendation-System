package com.example.demo.controller;

import com.example.demo.ml.HybridRecommendationService;
import com.example.demo.ml.RankedProduct;
import com.example.demo.ml.SearchResult;
import com.example.demo.ml.NLPSearchParser;
import com.example.demo.model.Product;
import com.example.demo.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class ProductController {

    private final ProductService              service;
    private final HybridRecommendationService hybridService;
    private final NLPSearchParser             nlpParser;

    public ProductController(ProductService service,
                             HybridRecommendationService hybridService,
                             NLPSearchParser nlpParser) {
        this.service       = service;
        this.hybridService = hybridService;
        this.nlpParser     = nlpParser;
    }

    // ===== HOME =====
    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        model.addAttribute("products", service.getFeaturedProducts());
        addSessionToModel(model, session);
        return "index";
    }

    // ===== ALL PRODUCTS / CATEGORY FILTER / SEARCH =====
    @GetMapping("/products")
    public String products(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            Model model,
            HttpSession session) {

        Integer userId = getSessionUserIdAsInteger(session);

        if (q != null && !q.isBlank()) {
            NLPSearchParser.ParsedQuery parsed = nlpParser.parse(q);
            SearchResult result = hybridService.search(q, userId);

            // ── Build a clean display label for the search query ──────────
            // Use resolved categories if the parser matched them,
            // otherwise fall back to the raw query trimmed to title-case.
            String displayQuery = buildDisplayQuery(q, parsed);

            model.addAttribute("searchQuery",        q);
            model.addAttribute("displayQuery",       displayQuery);   // ✅ NEW — used in template heading
            model.addAttribute("parsedCategories",   parsed.resolvedCategories);
            model.addAttribute("parsedBrands",       parsed.extractedBrands);
            model.addAttribute("forYouResults",      result.forYouProducts);
            model.addAttribute("hasForYou",          result.hasForYou());
            model.addAttribute("searchCategoryMap",  result.searchCategoryMap);
            model.addAttribute("hasSearchCategoryMap", result.hasSearchCategoryMap());
            model.addAttribute("products",           result.regularResults);
            model.addAttribute("totalCount",         result.totalCount());
            model.addAttribute("allCategories",      service.getAllCategories());
            model.addAttribute("isSearchMode",       true);
            model.addAttribute("isLoggedIn",         userId != null);

        } else {
            List<Product> products = service.filterProducts(category, null);
            model.addAttribute("products",             products);
            model.addAttribute("allCategories",        service.getAllCategories());
            model.addAttribute("category",             category);
            model.addAttribute("totalCount",           products.size());
            model.addAttribute("isSearchMode",         false);
            model.addAttribute("hasForYou",            false);
            model.addAttribute("hasSearchCategoryMap", false);
        }

        addSessionToModel(model, session);
        return "products";
    }

    // ===== PRODUCT DETAIL =====
    @GetMapping("/product-detail")
    public String productDetail(
            @RequestParam(required = false) Integer id,
            Model model,
            HttpSession session) {
        if (id != null) {
            model.addAttribute("product", service.getProductById(id));
        }
        addSessionToModel(model, session);
        return "product-detail";
    }

    // ===== FOR YOU PAGE =====
    @GetMapping("/for-you")
    public String forYou(HttpSession session, Model model) {
        Integer userId = getSessionUserIdAsInteger(session);

        if (userId == null) {
            model.addAttribute("isLoggedIn",         false);
            model.addAttribute("recommendations",    service.getFeaturedProducts());
            model.addAttribute("recommendationType", "popular");
            model.addAttribute("categoryMap",        Map.of());
            model.addAttribute("notEnoughOrders",    false);
        } else {
            Map<String, List<RankedProduct>> categoryMap =
                    hybridService.getForYouByCategory(userId);

            if (categoryMap.isEmpty()) {
                model.addAttribute("isLoggedIn",         true);
                model.addAttribute("recommendations",    service.getFeaturedProducts());
                model.addAttribute("recommendationType", "popular");
                model.addAttribute("categoryMap",        Map.of());
                model.addAttribute("notEnoughOrders",    true);
            } else {
                List<RankedProduct> flatList = new ArrayList<>();
                categoryMap.values().forEach(flatList::addAll);

                model.addAttribute("isLoggedIn",         true);
                model.addAttribute("categoryMap",        categoryMap);
                model.addAttribute("recommendationType", "personalized");
                model.addAttribute("notEnoughOrders",    false);
                model.addAttribute("recommendations",    flatList);
            }
        }

        model.addAttribute("topPhones",      service.getTopByCategory("Phones",         4));
        model.addAttribute("topFashion",     service.getTopByCategory("Womens Fashion", 4));
        model.addAttribute("topElectronics", service.getTopByCategory("Headphones",     4));

        addSessionToModel(model, session);
        return "for-you";
    }

    // ===== API =====
    @GetMapping("/api/recommendations")
    @ResponseBody
    public ResponseEntity<List<Product>> apiRecommendations(HttpSession session) {
        Integer userId = getSessionUserIdAsInteger(session);
        if (userId == null) return ResponseEntity.ok(service.getFeaturedProducts());
        Map<String, List<RankedProduct>> catMap = hybridService.getForYouByCategory(userId);
        if (catMap.isEmpty()) return ResponseEntity.ok(service.getFeaturedProducts());
        List<Product> products = new ArrayList<>();
        catMap.values().forEach(rps -> rps.forEach(rp -> {
            if (rp.getProduct() != null) products.add(rp.getProduct());
        }));
        return ResponseEntity.ok(products);
    }

    @GetMapping("/cart")
    public String cart(Model m, HttpSession s) {
        // ✅ Redirect to login if not authenticated
        if (s.getAttribute("userId") == null) return "redirect:/login";
        addSessionToModel(m, s);
        return "cart";
    }

    @GetMapping("/wishlist")
    public String wishlist(Model m, HttpSession s) {
        // ✅ Redirect to login if not authenticated
        if (s.getAttribute("userId") == null) return "redirect:/login";
        addSessionToModel(m, s);
        return "wishlist";
    }

    @GetMapping("/about")    public String about(Model m, HttpSession s)    { addSessionToModel(m, s); return "about"; }
    @GetMapping("/contact")  public String contact(Model m, HttpSession s)  { addSessionToModel(m, s); return "contact"; }
    @GetMapping("/faq")      public String faq(Model m, HttpSession s)      { addSessionToModel(m, s); return "faq"; }

    @GetMapping("/api/products")
    @ResponseBody
    public ResponseEntity<List<Product>> apiProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(service.filterProducts(category, q));
    }

    @GetMapping("/api/products/{id}")
    @ResponseBody
    public ResponseEntity<Product> apiProduct(@PathVariable Integer id) {
        Product p = service.getProductById(id);
        return p != null ? ResponseEntity.ok(p) : ResponseEntity.notFound().build();
    }

    // ===== HELPERS =====

    /**
     * Builds a human-readable display label for the search results heading.
     * Examples:
     *   "phones"          → "Phones"
     *   "best tvs"        → "Televisions"
     *   "show me cameras" → "Cameras"
     *   "buy something"   → "buy something"  (no category matched — use raw query)
     */
    private String buildDisplayQuery(String rawQuery, NLPSearchParser.ParsedQuery parsed) {
        if (parsed.resolvedCategories != null && !parsed.resolvedCategories.isEmpty()) {
            // Join multiple matched categories nicely: "Phones, Televisions"
            return String.join(", ", parsed.resolvedCategories);
        }
        // No category matched — title-case the raw query
        String trimmed = rawQuery.trim();
        if (trimmed.isEmpty()) return trimmed;
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1).toLowerCase();
    }

    private Integer getSessionUserIdAsInteger(HttpSession session) {
        Object raw = session.getAttribute("userId");
        if (raw == null)            return null;
        if (raw instanceof Long)    return ((Long) raw).intValue();
        if (raw instanceof Integer) return (Integer) raw;
        return null;
    }

    private void addSessionToModel(Model model, HttpSession session) {
        model.addAttribute("loggedIn",  session.getAttribute("userId") != null);
        model.addAttribute("userName",  session.getAttribute("userName"));
        model.addAttribute("userEmail", session.getAttribute("userEmail"));
    }
}
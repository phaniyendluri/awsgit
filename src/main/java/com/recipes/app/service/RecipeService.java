package com.recipes.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipes.app.dto.RecipeDto;
import com.recipes.app.dto.SearchResponseDto;
import com.recipes.app.model.RecipeCacheEntry;
import com.recipes.app.model.SearchAuditLog;
import com.recipes.app.repo.RecipeCacheRepository;
import com.recipes.app.repo.SearchAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecipeService {

    private static final Logger log = LoggerFactory.getLogger(RecipeService.class);

    private static final List<String> MEAT_WORDS = List.of("chicken", "beef", "pork", "fish", "bacon", "lamb", "shrimp", "ham", "turkey");
    private static final List<String> DAIRY_WORDS = List.of("milk", "cheese", "cream", "butter", "yogurt");
    private static final List<String> GLUTEN_WORDS = List.of("flour", "bread", "pasta", "soy sauce", "breadcrumbs", "noodle");

    private final RestTemplate restTemplate;
    private final RecipeCacheRepository cacheRepository;
    private final SearchAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public RecipeService(RestTemplate restTemplate,
                         RecipeCacheRepository cacheRepository,
                         SearchAuditLogRepository auditLogRepository,
                         ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.cacheRepository = cacheRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public SearchResponseDto search(String rawIngredients, String maxTime, String diet) {
        List<String> ingredients = parseIngredients(rawIngredients);
        if (ingredients.isEmpty()) {
            SearchResponseDto empty = new SearchResponseDto("Please enter at least one ingredient.", List.of(), false);
            saveAudit(ingredients, maxTime, diet, empty.recipes().size(), false);
            return empty;
        }

        String hash = buildQueryHash(ingredients, maxTime, diet);
        Optional<RecipeCacheEntry> cached = cacheRepository.findByQueryHashAndExpiresAtAfter(hash, Instant.now());
        if (cached.isPresent()) {
            try {
                SearchResponseDto response = objectMapper.readValue(cached.get().getPayloadJson(), SearchResponseDto.class);
                SearchResponseDto withCacheFlag = new SearchResponseDto(response.message(), response.recipes(), true);
                saveAudit(ingredients, maxTime, diet, withCacheFlag.recipes().size(), true);
                return withCacheFlag;
            } catch (Exception ex) {
                log.warn("Failed to deserialize cached payload for hash {}", hash, ex);
            }
        }

        SearchResponseDto freshResponse = fetchAndFilter(ingredients, maxTime, diet);
        saveCache(hash, freshResponse);
        saveAudit(ingredients, maxTime, diet, freshResponse.recipes().size(), false);
        return freshResponse;
    }

    private SearchResponseDto fetchAndFilter(List<String> ingredients, String maxTime, String diet) {
        try {
            String filterUrl = "https://www.themealdb.com/api/json/v1/1/filter.php?i=" + encode(ingredients.get(0));
            JsonNode filterData = objectMapper.readTree(restTemplate.getForObject(filterUrl, String.class));
            JsonNode meals = filterData.path("meals");
            if (!meals.isArray() || meals.isEmpty()) {
                return new SearchResponseDto("No matches found. Try another ingredient.", List.of(), false);
            }

            List<String> ids = new ArrayList<>();
            meals.forEach(node -> ids.add(node.path("idMeal").asText()));

            List<RecipeDto> recipes = ids.stream()
                    .limit(18)
                    .map(this::fetchMealDetail)
                    .filter(Objects::nonNull)
                    .filter(meal -> matchesIngredients(meal, ingredients))
                    .filter(meal -> matchesTime(meal, maxTime))
                    .filter(meal -> matchesDiet(meal, diet))
                    .collect(Collectors.toList());

            if (recipes.isEmpty()) {
                return new SearchResponseDto("Found recipes, but none matched your filters.", List.of(), false);
            }

            return new SearchResponseDto("Showing " + recipes.size() + " recipe(s).", recipes, false);
        } catch (Exception ex) {
            log.error("Recipe fetch failed", ex);
            return new SearchResponseDto("Could not load recipes right now. Please try again.", List.of(), false);
        }
    }

    private RecipeDto fetchMealDetail(String id) {
        try {
            String detailUrl = "https://www.themealdb.com/api/json/v1/1/lookup.php?i=" + id;
            JsonNode detailData = objectMapper.readTree(restTemplate.getForObject(detailUrl, String.class));
            JsonNode meal = detailData.path("meals").isArray() ? detailData.path("meals").get(0) : null;
            if (meal == null || meal.isMissingNode()) {
                return null;
            }

            List<String> ingredients = mealIngredients(meal);
            String instructions = meal.path("strInstructions").asText("");
            String haystack = buildHaystack(meal, ingredients);
            boolean vegetarian = !containsAny(haystack, MEAT_WORDS);
            boolean vegan = vegetarian && !containsAny(haystack, DAIRY_WORDS);
            boolean glutenFree = !containsAny(haystack, GLUTEN_WORDS);
            String summary = instructions.replaceAll("\\s+", " ").trim();
            if (summary.length() > 140) {
                summary = summary.substring(0, 140) + "...";
            }

            return new RecipeDto(
                    meal.path("idMeal").asText(),
                    meal.path("strMeal").asText(),
                    meal.path("strMealThumb").asText(),
                    preferredSource(meal),
                    summary,
                    estimateCookTime(instructions, ingredients.size()),
                    vegetarian,
                    vegan,
                    glutenFree,
                    ingredients
            );
        } catch (Exception ex) {
            log.warn("Failed to fetch/parse meal detail for id {}", id, ex);
            return null;
        }
    }

    private boolean matchesIngredients(RecipeDto meal, List<String> userIngredients) {
        List<String> recipeIngredients = meal.ingredients() == null ? List.of() : meal.ingredients();
        return userIngredients.stream().allMatch(input ->
                recipeIngredients.stream().anyMatch(item -> item.contains(input))
        );
    }

    private boolean matchesTime(RecipeDto recipe, String maxTime) {
        if (maxTime == null || maxTime.equals("all") || maxTime.isBlank()) return true;
        try {
            return recipe.estimatedMinutes() <= Integer.parseInt(maxTime);
        } catch (NumberFormatException ex) {
            return true;
        }
    }

    private boolean matchesDiet(RecipeDto recipe, String diet) {
        if (diet == null || diet.equals("any") || diet.isBlank()) return true;
        return switch (diet) {
            case "vegetarian" -> recipe.vegetarianFriendly();
            case "vegan" -> recipe.veganFriendly();
            case "gluten-free" -> recipe.glutenFreeFriendly();
            default -> true;
        };
    }

    private List<String> parseIngredients(String raw) {
        if (raw == null) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
    }

    private void saveCache(String hash, SearchResponseDto response) {
        try {
            RecipeCacheEntry entry = cacheRepository.findByQueryHash(hash).orElseGet(RecipeCacheEntry::new);
            entry.setQueryHash(hash);
            entry.setPayloadJson(objectMapper.writeValueAsString(response));
            entry.setCreatedAt(Instant.now());
            entry.setExpiresAt(Instant.now().plus(20, ChronoUnit.MINUTES));
            cacheRepository.save(entry);
        } catch (Exception ex) {
            log.warn("Failed to write cache for hash {}", hash, ex);
        }
    }

    private void saveAudit(List<String> ingredients, String maxTime, String diet, int resultCount, boolean cacheHit) {
        try {
            SearchAuditLog logEntry = new SearchAuditLog();
            logEntry.setIngredients(ingredients);
            logEntry.setMaxTime(maxTime);
            logEntry.setDiet(diet);
            logEntry.setResultCount(resultCount);
            logEntry.setCacheHit(cacheHit);
            logEntry.setCreatedAt(Instant.now());
            auditLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.warn("Failed to write audit log", ex);
        }
    }

    private String buildQueryHash(List<String> ingredients, String maxTime, String diet) {
        String source = String.join("|", new TreeSet<>(ingredients)) + "|" + maxTime + "|" + diet;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception ex) {
            return Integer.toHexString(source.hashCode());
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private int estimateCookTime(String instructions, int ingredientCount) {
        int words = instructions == null ? 0 : instructions.split("\\s+").length;
        return Math.max(10, Math.round(words / 4.0f + ingredientCount * 1.5f));
    }

    private List<String> mealIngredients(JsonNode meal) {
        List<String> ingredients = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            String value = meal.path("strIngredient" + i).asText("").trim().toLowerCase();
            if (!value.isBlank()) ingredients.add(value);
        }
        return ingredients;
    }

    private String buildHaystack(JsonNode meal, List<String> ingredients) {
        return (meal.path("strCategory").asText("") + " " +
                meal.path("strTags").asText("") + " " +
                meal.path("strInstructions").asText("") + " " +
                String.join(" ", ingredients)).toLowerCase();
    }

    private boolean containsAny(String haystack, List<String> terms) {
        return terms.stream().anyMatch(haystack::contains);
    }

    private String preferredSource(JsonNode meal) {
        String source = meal.path("strSource").asText("");
        if (!source.isBlank()) return source;
        String youtube = meal.path("strYoutube").asText("");
        if (!youtube.isBlank()) return youtube;
        return "#";
    }
}

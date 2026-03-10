package com.recipes.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipes.app.dto.SearchResponseDto;
import com.recipes.app.model.RecipeCacheEntry;
import com.recipes.app.repo.RecipeCacheRepository;
import com.recipes.app.repo.SearchAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RecipeCacheRepository cacheRepository;

    @Mock
    private SearchAuditLogRepository auditLogRepository;

    private RecipeService recipeService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        recipeService = new RecipeService(restTemplate, cacheRepository, auditLogRepository, objectMapper);
    }

    @Test
    void returnsValidationMessageWhenIngredientsAreEmpty() {
        SearchResponseDto response = recipeService.search("   ,  ", "all", "any");

        assertEquals("Please enter at least one ingredient.", response.message());
        assertTrue(response.recipes().isEmpty());
        assertFalse(response.cacheHit());
        verify(auditLogRepository, times(1)).save(any());
        verifyNoInteractions(restTemplate, cacheRepository);
    }

    @Test
    void returnsCachedResponseWhenUnexpiredCacheExists() throws Exception {
        SearchResponseDto cachedResponse = new SearchResponseDto("Showing 1 recipe(s).", List.of(), false);
        RecipeCacheEntry entry = new RecipeCacheEntry();
        entry.setQueryHash("hash");
        entry.setPayloadJson(objectMapper.writeValueAsString(cachedResponse));
        entry.setCreatedAt(Instant.now());
        entry.setExpiresAt(Instant.now().plusSeconds(600));

        when(cacheRepository.findByQueryHashAndExpiresAtAfter(anyString(), any(Instant.class)))
                .thenReturn(Optional.of(entry));

        SearchResponseDto response = recipeService.search("chicken", "all", "any");

        assertTrue(response.cacheHit());
        assertEquals("Showing 1 recipe(s).", response.message());
        verifyNoInteractions(restTemplate);
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    void filtersResultsUsingActualMealIngredients() {
        when(cacheRepository.findByQueryHashAndExpiresAtAfter(anyString(), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(cacheRepository.findByQueryHash(anyString())).thenReturn(Optional.empty());

        when(restTemplate.getForObject(contains("filter.php?i=chicken"), eq(String.class)))
                .thenReturn("""
                        {"meals":[{"idMeal":"1"},{"idMeal":"2"}]}
                        """);

        when(restTemplate.getForObject(contains("lookup.php?i=1"), eq(String.class)))
                .thenReturn("""
                        {"meals":[{"idMeal":"1","strMeal":"Chicken Onion Fry","strMealThumb":"img1","strSource":"src1",
                        "strInstructions":"Cook quickly","strIngredient1":"chicken","strIngredient2":"onion"}]}
                        """);

        when(restTemplate.getForObject(contains("lookup.php?i=2"), eq(String.class)))
                .thenReturn("""
                        {"meals":[{"idMeal":"2","strMeal":"Chicken Only","strMealThumb":"img2","strSource":"src2",
                        "strInstructions":"Cook slowly","strIngredient1":"chicken","strIngredient2":"pepper"}]}
                        """);

        SearchResponseDto response = recipeService.search("chicken,onion", "all", "any");

        assertEquals(1, response.recipes().size());
        assertEquals("1", response.recipes().get(0).id());
        assertTrue(response.recipes().get(0).ingredients().contains("onion"));
        verify(cacheRepository, times(1)).save(any());
        verify(auditLogRepository, times(1)).save(any());
    }
}

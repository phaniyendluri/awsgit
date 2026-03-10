package com.recipes.app.controller;

import com.recipes.app.dto.SearchResponseDto;
import com.recipes.app.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecipeService recipeService;

    @Test
    void delegatesQueryParamsToServiceAndReturnsJson() throws Exception {
        when(recipeService.search(eq("chicken,onion"), eq("45"), eq("vegetarian")))
                .thenReturn(new SearchResponseDto("Showing 0 recipe(s).", List.of(), false));

        mockMvc.perform(get("/api/recipes/search")
                        .param("ingredients", "chicken,onion")
                        .param("maxTime", "45")
                        .param("diet", "vegetarian"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Showing 0 recipe(s)."))
                .andExpect(jsonPath("$.recipes").isArray())
                .andExpect(jsonPath("$.cacheHit").value(false));

        verify(recipeService).search("chicken,onion", "45", "vegetarian");
    }
}

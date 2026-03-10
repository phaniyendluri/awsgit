package com.recipes.app.controller;

import com.recipes.app.dto.SearchResponseDto;
import com.recipes.app.service.RecipeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping("/search")
    public SearchResponseDto search(
            @RequestParam String ingredients,
            @RequestParam(defaultValue = "all") String maxTime,
            @RequestParam(defaultValue = "any") String diet
    ) {
        return recipeService.search(ingredients, maxTime, diet);
    }
}

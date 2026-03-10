package com.recipes.app.dto;

import java.util.List;

public record SearchResponseDto(
        String message,
        List<RecipeDto> recipes,
        boolean cacheHit
) {
}

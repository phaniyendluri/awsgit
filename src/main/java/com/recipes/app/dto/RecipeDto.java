package com.recipes.app.dto;

import java.util.List;

public record RecipeDto(
        String id,
        String name,
        String image,
        String sourceUrl,
        String summary,
        int estimatedMinutes,
        boolean vegetarianFriendly,
        boolean veganFriendly,
        boolean glutenFreeFriendly,
        List<String> ingredients
) {
}

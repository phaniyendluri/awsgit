package com.recipes.app.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "recipe_search_audit")
public class SearchAuditLog {

    @Id
    private String id;

    private List<String> ingredients;
    private String maxTime;
    private String diet;
    private Integer resultCount;
    private boolean cacheHit;
    private Instant createdAt;

    public String getId() { return id; }
    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }
    public String getMaxTime() { return maxTime; }
    public void setMaxTime(String maxTime) { this.maxTime = maxTime; }
    public String getDiet() { return diet; }
    public void setDiet(String diet) { this.diet = diet; }
    public Integer getResultCount() { return resultCount; }
    public void setResultCount(Integer resultCount) { this.resultCount = resultCount; }
    public boolean isCacheHit() { return cacheHit; }
    public void setCacheHit(boolean cacheHit) { this.cacheHit = cacheHit; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

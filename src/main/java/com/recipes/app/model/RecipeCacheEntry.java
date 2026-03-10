package com.recipes.app.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "recipe_cache", indexes = {
        @Index(name = "idx_recipe_cache_query_hash", columnList = "queryHash", unique = true)
})
public class RecipeCacheEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String queryHash;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public String getQueryHash() { return queryHash; }
    public void setQueryHash(String queryHash) { this.queryHash = queryHash; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

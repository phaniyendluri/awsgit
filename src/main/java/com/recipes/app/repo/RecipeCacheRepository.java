package com.recipes.app.repo;

import com.recipes.app.model.RecipeCacheEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface RecipeCacheRepository extends JpaRepository<RecipeCacheEntry, Long> {
    Optional<RecipeCacheEntry> findByQueryHashAndExpiresAtAfter(String queryHash, Instant now);
    Optional<RecipeCacheEntry> findByQueryHash(String queryHash);
}

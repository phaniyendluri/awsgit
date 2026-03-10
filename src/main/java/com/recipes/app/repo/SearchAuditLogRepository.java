package com.recipes.app.repo;

import com.recipes.app.model.SearchAuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SearchAuditLogRepository extends MongoRepository<SearchAuditLog, String> {
}

# What Should I Cook? (Java + JavaScript)

This project uses:
- **Java (Spring Boot)** backend API
- **Frontend JavaScript** for UI interactions
- **PostgreSQL** to cache API responses
- **MongoDB (NoSQL)** to store search audit records

## Features
- Ingredient input (comma-separated)
- Filters for max cooking time and diet
- Recipe cards with image and summary
- TheMealDB integration on backend
- PostgreSQL cache (20-minute TTL)
- MongoDB audit log for search requests
- Improved ingredient matching accuracy using real meal ingredient fields

## Run locally

### 1) Start databases
```bash
docker compose up -d
```

### 2) Run app
```bash
./mvnw spring-boot:run
```
If `mvnw` is unavailable, use:
```bash
mvn spring-boot:run
```

### 3) Open app
`http://localhost:8080`

## API endpoint
`GET /api/recipes/search?ingredients=chicken,onion&maxTime=45&diet=any`

## Storage behavior
- Final search results are cached in PostgreSQL table `recipe_cache`.
- Every search is saved in MongoDB collection `recipe_search_audit`.

## Production deployment inputs
See [PRODUCTION_READY_GUIDE.md](./PRODUCTION_READY_GUIDE.md) for free/low-cost hosting options, env vars, and hardening checklist.


## Tests
```bash
mvn test
```

# Production-Ready Guide (Free/Low-Cost Hosting)

This guide gives you a practical path to deploy this recipe finder in a production-style setup.

## 1) Architecture you should use
- **Frontend + API**: Spring Boot (serves static UI + `/api/recipes/search`)
- **Cache DB**: PostgreSQL (`recipe_cache`)
- **NoSQL event/audit store**: MongoDB (`recipe_search_audit`)
- **External data source**: TheMealDB (`filter.php` + `lookup.php`)

## 2) Accuracy of recipe endpoint
Your backend now:
- pulls candidate meal IDs from TheMealDB by first ingredient
- fetches meal details
- filters by matching **all user ingredients** against the actual meal ingredient list (not only title/summary)

This is as accurate as TheMealDB data allows. For higher commercial accuracy, migrate provider to Spoonacular/Edamam.

## 3) Free hosting options

### Option A (simple, mostly free to start)
- **Render Web Service** for Spring Boot app
- **Neon** for PostgreSQL (free tier)
- **MongoDB Atlas** for MongoDB (free shared tier)

### Option B (AWS-focused)
- **AWS App Runner** for app
- **RDS PostgreSQL**
- **MongoDB Atlas**

### Option C (truly free DIY)
- **Oracle Cloud Always Free VM**
- run app + Postgres + Mongo in Docker Compose on the VM

## 4) Required environment variables
Set these in your host:
- `POSTGRES_URL=jdbc:postgresql://<host>:5432/<db>`
- `POSTGRES_USER=<user>`
- `POSTGRES_PASSWORD=<password>`
- `MONGO_URI=mongodb+srv://<user>:<password>@<cluster>/<db>`
- `PORT=8080` (or host-defined)

## 5) Deployment checklist
1. Build app artifact (`mvn clean package`).
2. Provision Postgres + Mongo.
3. Configure env vars.
4. Deploy app.
5. Verify health and smoke test:
   - Open `/`
   - Call `/api/recipes/search?ingredients=chicken,onion&maxTime=45&diet=any`
6. Turn on logs and alerts.

## 6) Production hardening checklist
- Add `/actuator/health` for health checks.
- Add request timeout + retry policy around external API.
- Add rate limiter per IP.
- Add structured logging and centralized log aggregation.
- Add CI/CD pipeline with test stage + deploy stage.
- Add backups for Postgres and Mongo.

## 7) Cost note
No permanent platform is 100% free at scale. For a portfolio/demo app, free tiers are fine. For real traffic, expect paid plans.

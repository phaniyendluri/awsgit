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

## 3) Create a separate repository (from this codebase)
Use the helper script:
```bash
./scripts/create_separate_repo.sh ../recipe-finder-prod <your-new-git-url>
```

If you want to prepare locally first without pushing:
```bash
./scripts/create_separate_repo.sh ../recipe-finder-prod
```

The script:
- copies production app files to a new folder
- initializes a new git repo
- creates an initial commit
- optionally links/pushes to your new remote

## 4) Free hosting options

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

## 5) Required environment variables
Set these in your host:
- `POSTGRES_URL=jdbc:postgresql://<host>:5432/<db>`
- `POSTGRES_USER=<user>`
- `POSTGRES_PASSWORD=<password>`
- `MONGO_URI=mongodb+srv://<user>:<url_encoded_password>@<cluster-host>/?retryWrites=true&w=majority&appName=Cluster0`
- `MONGO_DB=recipes`
- `PORT=8080` (or host-defined)

Notes:
- If you prefer, you can include the DB in URI directly (`.../<db>?...`) and skip `MONGO_DB`.
- URL-encode MongoDB passwords that include special characters.

## 6) Deployment checklist
1. Build app artifact (`mvn clean package`).
2. Provision Postgres + Mongo.
3. Configure env vars.
4. Deploy app (Dockerfile included).
5. Verify health and smoke test:
   - Open `/`
   - Health: `/actuator/health`
   - API: `/api/recipes/search?ingredients=chicken,onion&maxTime=45&diet=any`
6. Turn on logs and alerts.

## 7) Production hardening checklist
- Keep `/actuator/health` as host health-check endpoint.
- Keep request timeout + retry policy around external API.
- Add rate limiter per IP (recommended next).
- Add centralized logs and alerts.
- Add CI/CD pipeline with test stage + deploy stage.
- Add backups for Postgres and Mongo.

## 8) Cost note
No permanent platform is 100% free at scale. For a portfolio/demo app, free tiers are fine. For real traffic, expect paid plans.

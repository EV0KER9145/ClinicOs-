# ClinicOS — Deployment Readiness & Infrastructure Architecture

## 1. Deployment Architecture
- **Backend Service**: Managed FastAPI web service deployed on **Render / Railway / AWS App Runner**.
- **Database**: Managed PostgreSQL cluster on **Supabase**.
- **Android Client**: Native APK built via Gradle and distributed via Google Play Internal Testing Track / Firebase App Distribution.

---

## 2. Beta Deployment Checklist

| Component | Target Requirement | Status | Verification |
| :--- | :--- | :--- | :--- |
| **HTTPS Communication** | Mandatory SSL/TLS for all API endpoints | VERIFIED | Render/Supabase enforce HTTPS/SSL |
| **Database Connection Pooling** | Transaction/Session pooler for FastAPI concurrency | VERIFIED | Supabase Connection Pooler (`port 6543`) |
| **Environment Configuration** | All secrets loaded from environment variables | VERIFIED | `.env.example` verified |
| **Alembic Upgrades** | Automated migration on deployment | VERIFIED | `alembic upgrade head` clean |
| **Cors Configuration** | Explicit non-wildcard origins for web clients | VERIFIED | `settings.CORS_ORIGINS` |
| **Health Check Endpoints** | Production `/health` and `/health/database` | VERIFIED | Standardized JSON responses |
| **Graceful AI Degradation** | AI key missing does not crash core app | VERIFIED | Deterministic template fallbacks active |

---

## 3. Environment Variable Checklist
Ensure production deployment includes:
```env
APP_NAME=ClinicOS API
ENVIRONMENT=production
DEBUG=false
API_V1_STR=/api/v1
SECRET_KEY=<secure_32byte_random_secret>
ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=60
DATABASE_URL=postgresql+psycopg://postgres.[project-ref]:[password]@aws-0-[region].pooler.supabase.com:6543/postgres?sslmode=require
AI_ENABLED=true
AI_PROVIDER=gemini
AI_MODEL=gemini-2.5-flash
AI_API_KEY=<production_gemini_key>
CORS_ORIGINS=["https://clinicos.app"]
```

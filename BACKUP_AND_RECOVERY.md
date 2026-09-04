# ClinicOS — Backup, Disaster Recovery & Database Resilience Strategy

## 1. Primary Database Hosting
ClinicOS uses **Supabase PostgreSQL** as its managed database cluster.

## 2. Backup Strategy
- **Point-in-Time Recovery (PITR)**: Provided natively by Supabase for production database tiers, enabling continuous WAL archiving and point-in-time recovery down to the second.
- **Daily Automated Physical Backups**: Daily database snapshots retained according to Supabase tier SLA.
- **Schema Migration Backups**: Alembic migration scripts (`alembic/versions/001_initial_schema.py` to `007_automation_module.py`) are version-controlled in Git, ensuring complete schema reproducibility on any new PostgreSQL instance.

---

## 3. Disaster Recovery & Rollback Procedure
1. **Database Unavailability**:
   - Backend `/health/database` endpoint immediately returns `HTTP 503 Service Unavailable`.
   - Android application detects network/503 errors and presents graceful retry UI without crashing.
2. **Schema Migration Rollback**:
   - Every Alembic migration revision includes both `upgrade()` and `downgrade()` steps.
   - Execute `alembic downgrade -1` to roll back the most recent migration safely.
3. **Application State Recovery**:
   - Stateless FastAPI instances on Render automatically restart if health checks fail.
   - Android client authentication tokens are securely persisted in `EncryptedSharedPreferences`, restoring user sessions upon reconnect.

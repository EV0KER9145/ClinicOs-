# ClinicOS — Beta Release Verification Checklist

## 1. Backend Verification
- [x] Persistent HTTPS API Endpoint deployed on Render (`https://clinicos-1mlt.onrender.com/`).
- [x] Database connection pooling active on Supabase PostgreSQL (`port 6543`).
- [x] All 7 Alembic migrations applied cleanly (`001_initial_schema` to `007_automation_module`).
- [x] Production `/health` and `/health/database` endpoints operational.
- [x] Environment variables securely configured without committed secrets.
- [x] All 56 backend pytest suite tests passing (100%).

---

## 2. Security Verification
- [x] Cross-tenant isolation verified across all entities (`patients`, `leads`, `appointments`, `follow_ups`, `notifications`, `automation_executions`, `communication_drafts`).
- [x] JWT expiration & signature verification active.
- [x] Deactivated clinic & user account access blocked (`HTTP 403 Forbidden`).
- [x] Medical safety boundary active in AI service (`MedicalSafetyChecker`).
- [x] Prompt injection sanitization applied to user text.

---

## 3. Android Client Verification
- [x] Version set to `0.1.0-beta` (`versionCode = 1`).
- [x] Release buildType uses persistent HTTPS API URL (`https://clinicos-1mlt.onrender.com/`).
- [x] JWT token stored in `EncryptedSharedPreferences`.
- [x] All runtime demo / sample data purged from codebase.
- [x] Professional empty states implemented for all screens.
- [x] Release compilation succeeded via `./gradlew assembleDebug` / `./gradlew assembleRelease`.

---

## 4. Documentation & Onboarding Readiness
- [x] `BETA_TESTER_GUIDE.md` created for non-technical clinic staff.
- [x] `DATABASE_CLEANUP_PROCEDURE.md` created for database purging.
- [x] `DATA_INVENTORY.md` and privacy assessments verified.

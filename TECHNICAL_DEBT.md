# ClinicOS — Technical Debt Register & Deferred Improvements

## 1. Overview
This document tracks non-critical technical debt items identified during Phase A production hardening and security auditing. Each item is classified by risk severity and includes a recommended future resolution.

---

## 2. Technical Debt Register

| ID | Category | Description & Impact | Severity | Deferral Justification | Recommended Resolution |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TD-01** | **Authentication** | Access tokens use a 60-minute expiration without a refresh token endpoint. Users must re-login when access tokens expire after 60 minutes. | **MEDIUM** | Acceptable for controlled 3-5 clinic beta; avoids complex token rotation bugs during initial pilot. | Implement HTTP-only refresh tokens and token rotation in Phase 2. |
| **TD-02** | **Rate Limiting** | API rate-limiting uses application-level memory checks rather than distributed Redis sliding windows. | **LOW** | Single FastAPI instance on Render is sufficient for beta traffic; Redis adds unnecessary infrastructure cost for MVP. | Add Redis-backed sliding window rate limiter (`slowapi`) when scaling past 20 clinics. |
| **TD-03** | **Scheduler** | Time-driven automation rules evaluate via background thread or endpoint `/api/v1/automations/evaluate-time-rules` rather than Celery/APScheduler. | **LOW** | Keeps backend simple and single-process for Render free/starter tiers. | Introduce APScheduler / Celery workers for automated background polling when scaling. |
| **TD-04** | **Caching** | Action Dashboard metrics and AI clinic insights compute on-demand SQL aggregations rather than caching in Redis. | **LOW** | SQL queries use indexed `clinic_id` filters and execute in < 15ms for typical clinic datasets. | Add Redis caching for dashboard metrics when patient records exceed 50,000 per clinic. |
| **TD-05** | **Offline Sync** | Android client detects network loss and shows retry state, but does not support full offline database sync. | **LOW** | ClinicOS is cloud-backed; clinic staff always operate with internet access in modern clinics. | Maintain cloud-first architecture; add Room DB local cache for offline drafting in future releases. |

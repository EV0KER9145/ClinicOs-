# ClinicOS — Database Cleanup Procedure for Beta Onboarding

## 1. Overview
Before onboarding real beta clinics onto ClinicOS, all synthetic test records created during development must be purged or segregated into a test clinic tenant to ensure a 100% clean starting state for real clinic users.

---

## 2. Recommended Database Strategy (Option A / C)
- **Beta Database Isolation**: Ensure real beta clinics register on the persistent production/beta database instance (`https://clinicos-1mlt.onrender.com/`).
- **Tenant Isolation Safeguard**: Since all ClinicOS database entities are strictly scoped by `clinic_id`, newly registered beta clinics automatically start with a completely empty, isolated dataset.

---

## 3. SQL Procedure to Purge Synthetic Development Records
If purging test development data from the Supabase database before beta launch, execute the following SQL script in the Supabase SQL Editor:

```sql
-- Step 1: Identify Synthetic Development Clinic ID(s)
-- SELECT id, name FROM clinics WHERE name ILIKE '%test%' OR name ILIKE '%dev%';

-- Step 2: Delete Tenant-Owned Records in Cascade Order for Test Clinic (e.g. 'YOUR_TEST_CLINIC_ID')
BEGIN;

DELETE FROM communication_drafts WHERE clinic_id = 'YOUR_TEST_CLINIC_ID';
DELETE FROM automation_executions WHERE clinic_id = 'YOUR_TEST_CLINIC_ID';
DELETE FROM notifications WHERE clinic_id = 'YOUR_TEST_CLINIC_ID';
DELETE FROM follow_ups WHERE clinic_id = 'YOUR_TEST_CLINIC_ID';
DELETE FROM appointments WHERE clinic_id = 'YOUR_TEST_CLINIC_ID';
DELETE FROM lead_notes WHERE clinic_id = 'YOUR_TEST_CLINIC_ID';
DELETE FROM leads WHERE clinic_id = 'YOUR_TEST_CLINIC_ID';
DELETE FROM patient_notes WHERE clinic_id = 'YOUR_TEST_CLINIC_ID';
DELETE FROM patient_tags WHERE patient_id IN (SELECT id FROM patients WHERE clinic_id = 'YOUR_TEST_CLINIC_ID');
DELETE FROM patients WHERE clinic_id = 'YOUR_TEST_CLINIC_ID';
DELETE FROM tags WHERE clinic_id = 'YOUR_TEST_CLINIC_ID';
DELETE FROM doctors WHERE clinic_id = 'YOUR_TEST_CLINIC_ID';
DELETE FROM users WHERE clinic_id = 'YOUR_TEST_CLINIC_ID';
DELETE FROM clinics WHERE id = 'YOUR_TEST_CLINIC_ID';

COMMIT;
```

---

## 4. Verification Checklist
After executing the cleanup procedure:
1. Register a new test clinic via the Android app or `/api/v1/auth/register` endpoint.
2. Log in and verify that all dashboard counts (`appointments_count`, `pending_follow_ups_count`, `new_leads_count`, `new_patients_count`) return `0`.
3. Verify that all lists (Patients, Leads, Appointments, Follow-ups, Notifications, Communication Drafts) display clean empty states with quick-action creation buttons.

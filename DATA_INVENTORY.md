# ClinicOS — Data Inventory & Privacy-by-Design Assessment

## 1. Overview & Data Minimization Strategy
ClinicOS is an Android-first, multi-tenant clinic CRM and practice-management platform. It is designed specifically for administrative workflows, patient retention, enquiry leads, follow-up callbacks, and scheduling. It is **NOT an Electronic Medical Record (EMR)** system and intentionally avoids storing diagnostic data, medical prescriptions, lab reports, or sensitive health records.

---

## 2. Category Inventory

| Data Category | Entities / Fields | Purpose | Retention Policy | Tenant Isolation |
| :--- | :--- | :--- | :--- | :--- |
| **Clinic Tenant Data** | `name`, `clinic_type`, `phone`, `email`, `timezone` | Tenant identification & local scheduling | Permanent while subscription active | Primary Tenant Root (`clinics.id`) |
| **User & Staff Credentials** | `full_name`, `email`, `password_hash`, `phone`, `role` | Authentication, RBAC (`OWNER`, `ADMIN`, `DOCTOR`, `STAFF`) | Permanent until staff account deletion | Enforced by `user.clinic_id` |
| **Doctor Profiles** | `full_name`, `specialty`, `is_active` | Appointment scheduling & doctor assignment | Soft deactivation (`is_active=False`) | Enforced by `doctor.clinic_id` |
| **Patient CRM Data** | `full_name`, `phone`, `email`, `date_of_birth`, `gender`, `address`, `notes` | Patient identity, search, and administrative history | Soft archive (`is_active=False`) | Enforced by `patient.clinic_id` |
| **Enquiry Leads Data** | `full_name`, `phone`, `email`, `source`, `status`, `interested_service`, `notes` | Patient acquisition & lead conversion | Retained until lead conversion or manual archive | Enforced by `lead.clinic_id` |
| **Appointments & Calendar** | `patient_id`, `doctor_id`, `scheduled_at`, `duration_minutes`, `status`, `notes` | Scheduling, conflict prevention, & no-show recovery | Permanent operational history | Enforced by `appointment.clinic_id` |
| **Follow-up Tasks** | `title`, `due_at`, `status`, `assigned_user_id`, `patient_id`, `lead_id`, `notes` | Callback task queue & receptionist workflow | Soft cancellation / retention after completion | Enforced by `follow_up.clinic_id` |
| **In-App Notifications** | `title`, `message`, `type`, `is_read`, `entity_type`, `entity_id` | Staff alerts and assignment notifications | Historical logs | Enforced by `clinic_id` AND `user_id` |
| **Automation Execution Logs** | `event_type`, `rule_type`, `action_type`, `idempotency_key`, `status` | System auditability & duplicate prevention | Rolling 90 days log retention | Enforced by `clinic_id` |
| **Communication Drafts** | `recipient_name`, `recipient_phone`, `purpose`, `message_content`, `status` | Prepared outreach message review & approval | Retained until draft status updated | Enforced by `clinic_id` |

---

## 3. Data Export & Tenant Deletion Readiness
- **Soft Deactivation Support**: Patients, Doctors, Users, and Leads support non-destructive soft archiving (`is_active=False`).
- **Data Export Pathway**: Database relationships are scoped directly to `clinic_id`, enabling atomic JSON/CSV export of a clinic's entire CRM dataset.
- **Tenant Deletion Safeguards**: Deleting a clinic record cascades strictly to associated tenant records without leaving orphan records.

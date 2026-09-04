# ClinicOS — Product Analytics Plan & Usage Tracking

## 1. Product Analytics Philosophy
Product analytics in ClinicOS is designed around **operational productivity events**. It tracks aggregated feature usage and workflow completions without ever capturing sensitive patient health content, notes, or messages.

---

## 2. Event Taxonomy

| Event Name | Trigger Condition | Properties Captured | Purpose |
| :--- | :--- | :--- | :--- |
| `USER_LOGGED_IN` | Staff signs into active session | `user_role` | Session engagement |
| `CLINIC_SETUP_COMPLETED` | Initial clinic setup saved | `clinic_type`, `timezone` | Onboarding conversion |
| `PATIENT_CREATED` | New patient registered | `has_phone`, `has_email`, `tag_count` | Patient CRM adoption |
| `LEAD_CAPTURED` | New enquiry lead recorded | `source`, `interested_service` | Patient acquisition tracking |
| `LEAD_CONVERTED` | Lead converted to patient | `converted_type` (`NEW` vs `EXISTING`) | Conversion rate tracking |
| `APPOINTMENT_BOOKED` | Appointment scheduled | `duration_minutes` | Calendar utilization |
| `APPOINTMENT_STATUS_UPDATED` | Status changed (`COMPLETED`, `NO_SHOW`, `CANCELLED`) | `old_status`, `new_status` | No-show & completion metrics |
| `FOLLOW_UP_CREATED` | Task scheduled | `related_to` (`PATIENT` vs `LEAD`) | Callback queue metrics |
| `FOLLOW_UP_COMPLETED` | Task marked completed | `was_overdue` | Task completion productivity |
| `AUTOMATION_TRIGGERED` | Rule executed | `rule_type`, `action_type` | Automation engine impact |
| `AI_MESSAGE_DRAFTED` | AI message generated | `purpose`, `tone` | AI feature engagement |
| `AI_ANALYTICS_QUERIED` | NL question asked | `intent` | Natural language query adoption |

---

## 3. Privacy Safeguards
- **No PII**: Patient names, phone numbers, addresses, and notes are strictly excluded from event properties.
- **Tenant Isolation**: Analytics aggregation queries enforce `clinic_id` scoping to prevent cross-tenant leakage.

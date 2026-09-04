from fastapi import APIRouter
from app.api.v1 import health, auth, clinics, doctors, users, patients, tags, leads, appointments

api_router = APIRouter()
api_router.include_router(health.router, tags=["Health"])
api_router.include_router(auth.router, prefix="/auth", tags=["Authentication"])
api_router.include_router(clinics.router, prefix="/clinics", tags=["Clinics"])
api_router.include_router(doctors.router, prefix="/doctors", tags=["Doctors"])
api_router.include_router(users.router, prefix="/users", tags=["Staff Management"])
api_router.include_router(patients.router, prefix="/patients", tags=["Patient CRM"])
api_router.include_router(tags.router, prefix="/tags", tags=["Tags"])
api_router.include_router(leads.router, prefix="/leads", tags=["Leads & Enquiries"])
api_router.include_router(appointments.router, prefix="/appointments", tags=["Appointments & Calendar"])

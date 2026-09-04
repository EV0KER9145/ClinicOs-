# ClinicOS Backend API

ClinicOS Backend is a production-oriented, modular monolith FastAPI REST service providing secure data management, business logic, and database persistence for the ClinicOS Android application.

---

## 🛠 Technology Stack

- **Framework**: [FastAPI](https://fastapi.tiangolo.com/) (Python 3.12+)
- **Server**: [Uvicorn](https://www.uvicorn.org/) (ASGI Server)
- **Database Provider**: [Supabase PostgreSQL](https://supabase.com/) (Hosted Cloud PostgreSQL 15+)
- **Database ORM**: [SQLAlchemy 2.0](https://www.sqlalchemy.org/)
- **Database Driver**: [Psycopg 3](https://www.psycopg.org/psycopg3/)
- **Database Migrations**: [Alembic](https://alembic.sqlalchemy.org/)
- **Security & JWT**: `pwdlib[bcrypt]` & `PyJWT`
- **Data Validation & Settings**: [Pydantic v2](https://docs.pydantic.dev/) & `pydantic-settings`
- **Testing**: [pytest](https://docs.pytest.org/) & `httpx`

---

## 📁 Project Structure

```
backend/
├── app/
│   ├── __init__.py
│   ├── main.py                  # FastAPI entry point & CORS configuration
│   ├── core/
│   │   ├── __init__.py
│   │   ├── config.py            # Typed Pydantic environment settings
│   │   ├── database.py          # SQLAlchemy engine, SSL & session factory
│   │   ├── security.py          # Bcrypt password hashing & JWT utilities
│   │   └── logging.py           # Structured application logging
│   ├── api/
│   │   ├── __init__.py
│   │   └── v1/
│   │       ├── __init__.py
│   │       ├── router.py        # Central v1 API router
│   │       ├── health.py        # Health & Database connectivity check endpoints
│   │       └── auth.py          # Register, Login & Current User endpoints
│   ├── dependencies/
│   │   └── auth.py              # Bearer token validation dependency
│   ├── models/
│   │   ├── __init__.py
│   │   ├── base.py              # DeclarativeBase, UUIDMixin, TimestampMixin
│   │   ├── clinic.py            # Clinic tenant model
│   │   ├── user.py              # User model & UserRole enum
│   │   └── doctor.py            # Doctor model
│   ├── schemas/
│   │   ├── __init__.py
│   │   ├── clinic.py            # Clinic request/response schemas
│   │   ├── user.py              # User request/response schemas
│   │   ├── doctor.py            # Doctor request/response schemas
│   │   └── auth.py              # Onboarding & Auth schemas
│   └── services/
│       └── auth_service.py      # Business logic & atomic registration
├── alembic/                      # Database migration scripts & env.py
├── tests/                       # Async & unit test suite
│   ├── __init__.py
│   ├── conftest.py              # TestClient & in-memory test DB fixtures
│   ├── test_health.py           # Health check tests
│   ├── test_models.py           # Model metadata tests
│   └── test_auth.py             # Auth & onboarding integration tests
├── alembic.ini                  # Alembic configuration
├── render.yaml                  # Render deployment configuration
├── .env.example                 # Template for environment variables
├── .gitignore                   # Version control ignore rules
├── requirements.txt             # Python dependencies
└── README.md                    # Backend documentation
```

---

## 🌐 Production Deployment on Render

The ClinicOS backend is configured for simple, portable deployment to cloud platforms like [Render](https://render.com/).

### Deployment Steps

1. **Push Code to Git Repository**:
   Ensure your ClinicOS repository is pushed to GitHub/GitLab.

2. **Create New Web Service on Render**:
   - Log into your [Render Dashboard](https://dashboard.render.com/) and click **New + -> Web Service**.
   - Connect your Git repository.
   - Set **Root Directory** to `backend`.
   - Set **Environment** to `Python 3`.
   - Set **Build Command**: `pip install -r requirements.txt`
   - Set **Start Command**: `uvicorn app.main:app --host 0.0.0.0 --port $PORT`

3. **Configure Environment Variables on Render**:
   Add the following environment variables in the Render Dashboard (**Environment** tab):

   | Variable | Value / Format | Description |
   | :--- | :--- | :--- |
   | `ENVIRONMENT` | `production` | Sets application mode to production |
   | `DEBUG` | `false` | Disables debug logs & verbose SQL output |
   | `SECRET_KEY` | *(Generate via `python -c "import secrets; print(secrets.token_hex(32))"`)* | Secret key for signing JWT tokens |
   | `ALGORITHM` | `HS256` | JWT signing algorithm |
   | `ACCESS_TOKEN_EXPIRE_MINUTES` | `60` | Token expiration duration |
   | `DATABASE_URL` | `postgresql+psycopg://postgres.[project-ref]:[password]@aws-0-[region].pooler.supabase.com:6543/postgres?sslmode=require` | Supabase PostgreSQL Connection String |
   | `CORS_ORIGINS` | `["https://your-frontend-domain.com"]` | Allowed CORS origins for web clients |

4. **Run Database Migrations Against Supabase**:
   Before or immediately after deploying the API, run the database migrations from your terminal:
   ```powershell
   cd backend
   $env:PYTHONPATH="."
   $env:DATABASE_URL="postgresql+psycopg://postgres:[password]@db.[project-ref].supabase.co:5432/postgres?sslmode=require"
   .\.venv\Scripts\alembic.exe upgrade head
   ```

5. **Verify Public Endpoint**:
   Once Render completes deployment, verify health at your public HTTPS URL:
   `https://your-service-name.onrender.com/api/v1/health`

---

## ⚡ Supabase PostgreSQL Setup Guide

ClinicOS uses Supabase strictly as its managed cloud PostgreSQL database provider. The FastAPI backend serves as the exclusive API and business logic layer for the Android client.

### Connection Architecture
```
Android Application
        │
        ▼ (HTTPS REST API)
FastAPI Backend (Render Cloud / Local)
        │
        ▼ (SQLAlchemy ORM + SSL)
Supabase PostgreSQL Database
```

> **Security Requirement**: Supabase database credentials belong exclusively in the backend `.env` configuration. They are **never** embedded or referenced inside the Android application.

---

## 🚀 Local Development Setup

### 1. Create Python Virtual Environment

Open PowerShell inside `ClinicOS/backend`:

```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
```

### 2. Install Dependencies

```powershell
pip install -r requirements.txt
```

### 3. Configure Environment Variables

Copy `.env.example` to `.env`:

```powershell
Copy-Item .env.example .env
```

Update `.env` with your Supabase database connection string and secret key.

### 4. Run Development Server

```powershell
$env:PYTHONPATH="."
.\.venv\Scripts\uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

---

## 🔍 Health & Database Verification

### API Health Endpoints
- **General Health**: `GET http://127.0.0.1:8000/api/v1/health`
- **Database Connectivity Health**: `GET http://127.0.0.1:8000/api/v1/health/database`
  Response when connected:
  ```json
  {
    "status": "healthy",
    "database": "connected"
  }
  ```

### Running Automated Test Suite

```powershell
$env:PYTHONPATH="."
.\.venv\Scripts\pytest
```

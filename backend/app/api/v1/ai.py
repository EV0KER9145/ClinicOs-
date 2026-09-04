from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session
from app.ai.schemas import (
    AnalyticsQueryRequest,
    AnalyticsQueryResponse,
    ClinicInsightsResponse,
    FollowUpRecommendationsResponse,
    GenerateMessageRequest,
    GenerateMessageResponse,
)
from app.ai.service import AIService
from app.core.database import get_db
from app.dependencies.auth import get_current_user
from app.models.user import User

router = APIRouter()
ai_service = AIService()


@router.post("/follow-up-recommendations", response_model=FollowUpRecommendationsResponse)
def get_ai_follow_up_recommendations(
    limit: int = Query(10, ge=1, le=50),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> FollowUpRecommendationsResponse:
    """C1 — AI Follow-up Intelligence: Generate prioritized action recommendations for tasks, no-shows, and leads."""
    return ai_service.get_follow_up_recommendations(db, current_user, limit=limit)


@router.post("/generate-message", response_model=GenerateMessageResponse)
def generate_ai_message(
    request_in: GenerateMessageRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> GenerateMessageResponse:
    """C2 — AI Message Assistant: Draft tailored administrative communication for patients, leads, or tasks."""
    return ai_service.generate_message(
        db=db,
        current_user=current_user,
        entity_type=request_in.entity_type,
        entity_id=request_in.entity_id,
        purpose=request_in.purpose,
        tone=request_in.tone,
        additional_context=request_in.additional_context
    )


@router.get("/clinic-insights", response_model=ClinicInsightsResponse)
def get_ai_clinic_insights(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> ClinicInsightsResponse:
    """C3 — AI Clinic Insights: Interpret operational patterns and deliver proactive recommendations."""
    return ai_service.get_clinic_insights(db, current_user)


@router.post("/analytics/query", response_model=AnalyticsQueryResponse)
def query_natural_language_analytics(
    request_in: AnalyticsQueryRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> AnalyticsQueryResponse:
    """C4 — Natural Language Analytics: Answer administrative clinic questions using validated, deterministic queries."""
    return ai_service.query_analytics(
        db=db,
        current_user=current_user,
        question=request_in.question,
        period=request_in.period
    )

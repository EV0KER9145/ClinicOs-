import uuid
from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import desc
from sqlalchemy.orm import Session
from app.automation.service import AutomationService
from app.core.database import get_db
from app.dependencies.auth import get_current_user
from app.models.automation import AutomationExecution, CommunicationDraft, DraftStatus
from app.models.user import User
from app.schemas.automation import (
    AutomationExecutionListResponse,
    CommunicationDraftListResponse,
    CommunicationDraftResponse,
    DraftStatusUpdate,
)

router = APIRouter()


@router.post("/evaluate-time-rules")
def evaluate_time_driven_automation_rules(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Evaluate scheduled time-driven automation rules for the authenticated user's clinic."""
    executed_count = AutomationService.evaluate_time_driven_rules(db, current_user.clinic_id)
    return {
        "message": "Time-driven automation rules evaluated successfully.",
        "rules_executed": executed_count
    }


@router.get("/executions", response_model=AutomationExecutionListResponse)
def list_automation_executions(
    page: int = Query(1, ge=1),
    page_size: int = Query(50, ge=1, le=100),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> AutomationExecutionListResponse:
    """List automation execution history for auditability and debugging."""
    query = db.query(AutomationExecution).filter(
        AutomationExecution.clinic_id == current_user.clinic_id
    )

    total = query.count()
    items = (
        query.order_by(desc(AutomationExecution.executed_at))
        .offset((page - 1) * page_size)
        .limit(page_size)
        .all()
    )

    return AutomationExecutionListResponse(
        items=items,
        total=total,
        page=page,
        page_size=page_size
    )


@router.get("/drafts", response_model=CommunicationDraftListResponse)
def list_communication_drafts(
    page: int = Query(1, ge=1),
    page_size: int = Query(50, ge=1, le=100),
    draft_status: Optional[DraftStatus] = Query(None, alias="status", description="Filter by draft status"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> CommunicationDraftListResponse:
    """List prepared communication drafts for staff review."""
    query = db.query(CommunicationDraft).filter(
        CommunicationDraft.clinic_id == current_user.clinic_id
    )

    if draft_status:
        query = query.filter(CommunicationDraft.status == draft_status)

    total = query.count()
    items = (
        query.order_by(desc(CommunicationDraft.created_at))
        .offset((page - 1) * page_size)
        .limit(page_size)
        .all()
    )

    return CommunicationDraftListResponse(
        items=items,
        total=total,
        page=page,
        page_size=page_size
    )


@router.patch("/drafts/{draft_id}/status", response_model=CommunicationDraftResponse)
def update_communication_draft_status(
    draft_id: uuid.UUID,
    status_in: DraftStatusUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> CommunicationDraftResponse:
    """Approve or discard a prepared communication draft."""
    draft = db.query(CommunicationDraft).filter(
        CommunicationDraft.id == draft_id,
        CommunicationDraft.clinic_id == current_user.clinic_id
    ).first()

    if not draft:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Communication draft not found."
        )

    draft.status = status_in.status
    db.add(draft)
    db.commit()
    db.refresh(draft)
    return draft

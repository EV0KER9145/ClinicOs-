"""create automation_executions and communication_drafts tables

Revision ID: 007_automation_module
Revises: 006_notifications_module
Create Date: 2026-09-04 15:05:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision: str = '007_automation_module'
down_revision: Union[str, None] = '006_notifications_module'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Create execution_status_enum and draft_status_enum
    execution_status_enum = postgresql.ENUM('SUCCESS', 'FAILED', 'SKIPPED', name='execution_status_enum', create_type=True)
    draft_status_enum = postgresql.ENUM('DRAFT', 'APPROVED', 'DISCARDED', 'SENT', name='draft_status_enum', create_type=True)

    # 2. Create automation_executions table
    op.create_table(
        'automation_executions',
        sa.Column('id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('clinic_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('event_type', sa.String(length=100), nullable=False),
        sa.Column('rule_type', sa.String(length=100), nullable=False),
        sa.Column('entity_type', sa.String(length=50), nullable=False),
        sa.Column('entity_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('action_type', sa.String(length=100), nullable=False),
        sa.Column('status', execution_status_enum, nullable=False, server_default='SUCCESS'),
        sa.Column('idempotency_key', sa.String(length=255), nullable=False),
        sa.Column('executed_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('error_message', sa.Text(), nullable=True),
        sa.ForeignKeyConstraint(['clinic_id'], ['clinics.id'], ondelete='RESTRICT'),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('idempotency_key', name='uq_automation_idempotency_key')
    )
    op.create_index(op.f('ix_automation_executions_clinic_id'), 'automation_executions', ['clinic_id'], unique=False)
    op.create_index(op.f('ix_automation_executions_event_type'), 'automation_executions', ['event_type'], unique=False)
    op.create_index(op.f('ix_automation_executions_rule_type'), 'automation_executions', ['rule_type'], unique=False)
    op.create_index(op.f('ix_automation_executions_entity_id'), 'automation_executions', ['entity_id'], unique=False)
    op.create_index(op.f('ix_automation_executions_idempotency_key'), 'automation_executions', ['idempotency_key'], unique=False)

    # 3. Create communication_drafts table
    op.create_table(
        'communication_drafts',
        sa.Column('id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('clinic_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('entity_type', sa.String(length=50), nullable=False),
        sa.Column('entity_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('recipient_name', sa.String(length=255), nullable=False),
        sa.Column('recipient_phone', sa.String(length=20), nullable=True),
        sa.Column('purpose', sa.String(length=100), nullable=False),
        sa.Column('message_content', sa.Text(), nullable=False),
        sa.Column('status', draft_status_enum, nullable=False, server_default='DRAFT'),
        sa.Column('created_by_user_id', postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(['clinic_id'], ['clinics.id'], ondelete='RESTRICT'),
        sa.ForeignKeyConstraint(['created_by_user_id'], ['users.id'], ondelete='SET NULL'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_communication_drafts_clinic_id'), 'communication_drafts', ['clinic_id'], unique=False)
    op.create_index(op.f('ix_communication_drafts_entity_id'), 'communication_drafts', ['entity_id'], unique=False)
    op.create_index(op.f('ix_communication_drafts_status'), 'communication_drafts', ['status'], unique=False)


def downgrade() -> None:
    op.drop_index(op.f('ix_communication_drafts_status'), table_name='communication_drafts')
    op.drop_index(op.f('ix_communication_drafts_entity_id'), table_name='communication_drafts')
    op.drop_index(op.f('ix_communication_drafts_clinic_id'), table_name='communication_drafts')
    op.drop_table('communication_drafts')

    op.drop_index(op.f('ix_automation_executions_idempotency_key'), table_name='automation_executions')
    op.drop_index(op.f('ix_automation_executions_entity_id'), table_name='automation_executions')
    op.drop_index(op.f('ix_automation_executions_rule_type'), table_name='automation_executions')
    op.drop_index(op.f('ix_automation_executions_event_type'), table_name='automation_executions')
    op.drop_index(op.f('ix_automation_executions_clinic_id'), table_name='automation_executions')
    op.drop_table('automation_executions')

    draft_status_enum = postgresql.ENUM('DRAFT', 'APPROVED', 'DISCARDED', 'SENT', name='draft_status_enum')
    draft_status_enum.drop(op.get_bind(), checkfirst=True)

    execution_status_enum = postgresql.ENUM('SUCCESS', 'FAILED', 'SKIPPED', name='execution_status_enum')
    execution_status_enum.drop(op.get_bind(), checkfirst=True)

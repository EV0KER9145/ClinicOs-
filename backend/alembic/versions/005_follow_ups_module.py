"""create follow_ups table

Revision ID: 005_follow_ups_module
Revises: 004_appointments_module
Create Date: 2026-09-04 13:35:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision: str = '005_follow_ups_module'
down_revision: Union[str, None] = '004_appointments_module'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Create follow_up_status_enum
    follow_up_status_enum = postgresql.ENUM('PENDING', 'COMPLETED', 'CANCELLED', name='follow_up_status_enum', create_type=True)

    # 2. Create follow_ups table
    op.create_table(
        'follow_ups',
        sa.Column('id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('clinic_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('assigned_user_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('patient_id', postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column('lead_id', postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column('title', sa.String(length=255), nullable=False),
        sa.Column('notes', sa.Text(), nullable=True),
        sa.Column('due_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('status', follow_up_status_enum, nullable=False, server_default='PENDING'),
        sa.Column('completed_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('cancelled_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('is_active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(['clinic_id'], ['clinics.id'], ondelete='RESTRICT'),
        sa.ForeignKeyConstraint(['assigned_user_id'], ['users.id'], ondelete='RESTRICT'),
        sa.ForeignKeyConstraint(['patient_id'], ['patients.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['lead_id'], ['leads.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_follow_ups_clinic_id'), 'follow_ups', ['clinic_id'], unique=False)
    op.create_index(op.f('ix_follow_ups_assigned_user_id'), 'follow_ups', ['assigned_user_id'], unique=False)
    op.create_index(op.f('ix_follow_ups_patient_id'), 'follow_ups', ['patient_id'], unique=False)
    op.create_index(op.f('ix_follow_ups_lead_id'), 'follow_ups', ['lead_id'], unique=False)
    op.create_index(op.f('ix_follow_ups_due_at'), 'follow_ups', ['due_at'], unique=False)
    op.create_index(op.f('ix_follow_ups_status'), 'follow_ups', ['status'], unique=False)


def downgrade() -> None:
    op.drop_index(op.f('ix_follow_ups_status'), table_name='follow_ups')
    op.drop_index(op.f('ix_follow_ups_due_at'), table_name='follow_ups')
    op.drop_index(op.f('ix_follow_ups_lead_id'), table_name='follow_ups')
    op.drop_index(op.f('ix_follow_ups_patient_id'), table_name='follow_ups')
    op.drop_index(op.f('ix_follow_ups_assigned_user_id'), table_name='follow_ups')
    op.drop_index(op.f('ix_follow_ups_clinic_id'), table_name='follow_ups')
    op.drop_table('follow_ups')

    follow_up_status_enum = postgresql.ENUM('PENDING', 'COMPLETED', 'CANCELLED', name='follow_up_status_enum')
    follow_up_status_enum.drop(op.get_bind(), checkfirst=True)

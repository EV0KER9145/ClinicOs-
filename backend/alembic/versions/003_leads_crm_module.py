"""create leads and lead_notes tables

Revision ID: 003_leads_crm_module
Revises: 002_patient_crm_foundation
Create Date: 2026-09-04 12:50:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision: str = '003_leads_crm_module'
down_revision: Union[str, None] = '002_patient_crm_foundation'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Create lead_source_enum and lead_status_enum
    lead_source_enum = postgresql.ENUM('PHONE', 'WHATSAPP', 'INSTAGRAM', 'GOOGLE', 'REFERRAL', 'WALK_IN', 'WEBSITE', 'OTHER', name='lead_source_enum', create_type=True)
    lead_status_enum = postgresql.ENUM('NEW', 'CONTACTED', 'INTERESTED', 'APPOINTMENT_BOOKED', 'CONVERTED', 'LOST', name='lead_status_enum', create_type=True)

    # 2. Create leads table
    op.create_table(
        'leads',
        sa.Column('id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('clinic_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('full_name', sa.String(length=255), nullable=False),
        sa.Column('phone', sa.String(length=20), nullable=True),
        sa.Column('email', sa.String(length=255), nullable=True),
        sa.Column('source', lead_source_enum, nullable=False, server_default='PHONE'),
        sa.Column('status', lead_status_enum, nullable=False, server_default='NEW'),
        sa.Column('assigned_user_id', postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column('interested_service', sa.String(length=255), nullable=True),
        sa.Column('notes', sa.Text(), nullable=True),
        sa.Column('converted_patient_id', postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column('converted_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('is_active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(['clinic_id'], ['clinics.id'], ondelete='RESTRICT'),
        sa.ForeignKeyConstraint(['assigned_user_id'], ['users.id'], ondelete='SET NULL'),
        sa.ForeignKeyConstraint(['converted_patient_id'], ['patients.id'], ondelete='SET NULL'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_leads_clinic_id'), 'leads', ['clinic_id'], unique=False)
    op.create_index(op.f('ix_leads_full_name'), 'leads', ['full_name'], unique=False)
    op.create_index(op.f('ix_leads_phone'), 'leads', ['phone'], unique=False)
    op.create_index(op.f('ix_leads_source'), 'leads', ['source'], unique=False)
    op.create_index(op.f('ix_leads_status'), 'leads', ['status'], unique=False)
    op.create_index(op.f('ix_leads_assigned_user_id'), 'leads', ['assigned_user_id'], unique=False)
    op.create_index(op.f('ix_leads_converted_patient_id'), 'leads', ['converted_patient_id'], unique=False)

    # 3. Create lead_notes table
    op.create_table(
        'lead_notes',
        sa.Column('id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('clinic_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('lead_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('author_user_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('content', sa.Text(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(['clinic_id'], ['clinics.id'], ondelete='RESTRICT'),
        sa.ForeignKeyConstraint(['lead_id'], ['leads.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['author_user_id'], ['users.id'], ondelete='RESTRICT'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_lead_notes_clinic_id'), 'lead_notes', ['clinic_id'], unique=False)
    op.create_index(op.f('ix_lead_notes_lead_id'), 'lead_notes', ['lead_id'], unique=False)
    op.create_index(op.f('ix_lead_notes_author_user_id'), 'lead_notes', ['author_user_id'], unique=False)


def downgrade() -> None:
    op.drop_index(op.f('ix_lead_notes_author_user_id'), table_name='lead_notes')
    op.drop_index(op.f('ix_lead_notes_lead_id'), table_name='lead_notes')
    op.drop_index(op.f('ix_lead_notes_clinic_id'), table_name='lead_notes')
    op.drop_table('lead_notes')

    op.drop_index(op.f('ix_leads_converted_patient_id'), table_name='leads')
    op.drop_index(op.f('ix_leads_assigned_user_id'), table_name='leads')
    op.drop_index(op.f('ix_leads_status'), table_name='leads')
    op.drop_index(op.f('ix_leads_source'), table_name='leads')
    op.drop_index(op.f('ix_leads_phone'), table_name='leads')
    op.drop_index(op.f('ix_leads_full_name'), table_name='leads')
    op.drop_index(op.f('ix_leads_clinic_id'), table_name='leads')
    op.drop_table('leads')

    lead_status_enum = postgresql.ENUM('NEW', 'CONTACTED', 'INTERESTED', 'APPOINTMENT_BOOKED', 'CONVERTED', 'LOST', name='lead_status_enum')
    lead_status_enum.drop(op.get_bind(), checkfirst=True)

    lead_source_enum = postgresql.ENUM('PHONE', 'WHATSAPP', 'INSTAGRAM', 'GOOGLE', 'REFERRAL', 'WALK_IN', 'WEBSITE', 'OTHER', name='lead_source_enum')
    lead_source_enum.drop(op.get_bind(), checkfirst=True)

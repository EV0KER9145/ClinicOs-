"""create patients tags patient_tags patient_notes tables

Revision ID: 002_patient_crm_foundation
Revises: 001_initial_schema
Create Date: 2026-09-04 12:30:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision: str = '002_patient_crm_foundation'
down_revision: Union[str, None] = '001_initial_schema'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Create patients table
    op.create_table(
        'patients',
        sa.Column('id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('clinic_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('full_name', sa.String(length=255), nullable=False),
        sa.Column('phone', sa.String(length=20), nullable=True),
        sa.Column('email', sa.String(length=255), nullable=True),
        sa.Column('date_of_birth', sa.Date(), nullable=True),
        sa.Column('gender', sa.String(length=20), nullable=True),
        sa.Column('address', sa.Text(), nullable=True),
        sa.Column('notes', sa.Text(), nullable=True),
        sa.Column('is_active', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(['clinic_id'], ['clinics.id'], ondelete='RESTRICT'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_patients_clinic_id'), 'patients', ['clinic_id'], unique=False)
    op.create_index(op.f('ix_patients_full_name'), 'patients', ['full_name'], unique=False)
    op.create_index(op.f('ix_patients_phone'), 'patients', ['phone'], unique=False)

    # 2. Create tags table
    op.create_table(
        'tags',
        sa.Column('id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('clinic_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('name', sa.String(length=100), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(['clinic_id'], ['clinics.id'], ondelete='RESTRICT'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_tags_clinic_id'), 'tags', ['clinic_id'], unique=False)

    # 3. Create patient_tags association table
    op.create_table(
        'patient_tags',
        sa.Column('patient_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('tag_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.ForeignKeyConstraint(['patient_id'], ['patients.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['tag_id'], ['tags.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('patient_id', 'tag_id')
    )

    # 4. Create patient_notes table
    op.create_table(
        'patient_notes',
        sa.Column('id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('clinic_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('patient_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('author_user_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('content', sa.Text(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(['clinic_id'], ['clinics.id'], ondelete='RESTRICT'),
        sa.ForeignKeyConstraint(['patient_id'], ['patients.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['author_user_id'], ['users.id'], ondelete='RESTRICT'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_patient_notes_clinic_id'), 'patient_notes', ['clinic_id'], unique=False)
    op.create_index(op.f('ix_patient_notes_patient_id'), 'patient_notes', ['patient_id'], unique=False)
    op.create_index(op.f('ix_patient_notes_author_user_id'), 'patient_notes', ['author_user_id'], unique=False)


def downgrade() -> None:
    op.drop_index(op.f('ix_patient_notes_author_user_id'), table_name='patient_notes')
    op.drop_index(op.f('ix_patient_notes_patient_id'), table_name='patient_notes')
    op.drop_index(op.f('ix_patient_notes_clinic_id'), table_name='patient_notes')
    op.drop_table('patient_notes')

    op.drop_table('patient_tags')

    op.drop_index(op.f('ix_tags_clinic_id'), table_name='tags')
    op.drop_table('tags')

    op.drop_index(op.f('ix_patients_phone'), table_name='patients')
    op.drop_index(op.f('ix_patients_full_name'), table_name='patients')
    op.drop_index(op.f('ix_patients_clinic_id'), table_name='patients')
    op.drop_table('patients')

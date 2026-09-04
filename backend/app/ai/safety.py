import re
from typing import Tuple

# Prohibited clinical & medical terms to enforce safety boundary
PROHIBITED_MEDICAL_TERMS = [
    r"\bdiagnos(?:e|is|es|ing)\b",
    r"\btreat(?:ment|ments|ing)\b",
    r"\bprescr(?:ibe|iption|iptions)\b",
    r"\bmedicin(?:e|es|ation|ations)\b",
    r"\bdosag(?:e|es)\b",
    r"\bsymptom(?:s)?\b",
    r"\bdisord(?:er|ers)\b",
    r"\bdiseas(?:e|es)\b",
    r"\bsurger(?:y|ies)\b",
    r"\btherap(?:y|ies)\b",
    r"\bcure\b",
    r"\bpathology\b",
    r"\blab report\b"
]


class MedicalSafetyChecker:

    @staticmethod
    def is_clinical_query(text: str) -> bool:
        """Check if the input text contains clinical or medical queries."""
        text_lower = text.lower()
        for pattern in PROHIBITED_MEDICAL_TERMS:
            if re.search(pattern, text_lower):
                return True
        return False

    @staticmethod
    def sanitize_untrusted_text(text: str) -> str:
        """Sanitize user-entered database text to prevent prompt injection or instruction overrides."""
        if not text:
            return ""

        # Strip instructions overrides like 'SYSTEM:', 'IGNORE INSTRUCTIONS', etc.
        sanitized = re.sub(r"(?i)(system:|instructions:|ignore previous instructions)", "", text)
        return sanitized.strip()

    @staticmethod
    def validate_clinical_boundary(question: str) -> Tuple[bool, str]:
        """Validate if a natural language question crosses the medical safety boundary."""
        if MedicalSafetyChecker.is_clinical_query(question):
            return True, (
                "ClinicOS AI is strictly an administrative and practice-management assistant. "
                "It does not provide medical diagnoses, treatment recommendations, or clinical advice."
            )
        return False, ""

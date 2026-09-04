from abc import ABC, abstractmethod
from typing import Any, Dict, Optional, Type


class BaseAIProvider(ABC):

    @abstractmethod
    def generate_text(
        self,
        prompt: str,
        system_instruction: str = ""
    ) -> Optional[str]:
        """Generate text completion from AI model."""
        pass

    @abstractmethod
    def generate_json(
        self,
        prompt: str,
        system_instruction: str = ""
    ) -> Optional[Dict[str, Any]]:
        """Generate structured JSON object from AI model."""
        pass

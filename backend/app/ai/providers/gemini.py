import json
import logging
from typing import Any, Dict, Optional
import httpx
from app.ai.providers.base import BaseAIProvider
from app.core.config import settings

logger = logging.getLogger("clinicos.ai")


class GeminiAIProvider(BaseAIProvider):

    def __init__(self):
        self.api_key = settings.AI_API_KEY
        self.model = settings.AI_MODEL or "gemini-2.5-flash"

    def generate_text(
        self,
        prompt: str,
        system_instruction: str = ""
    ) -> Optional[str]:
        if not settings.AI_ENABLED or not self.api_key:
            logger.info("AI Provider disabled or missing AI_API_KEY. Using fallback execution.")
            return None

        url = f"https://generativelanguage.googleapis.com/v1beta/models/{self.model}:generateContent?key={self.api_key}"
        payload = {
            "contents": [
                {
                    "parts": [{"text": f"{system_instruction}\n\n{prompt}"}]
                }
            ]
        }

        try:
            with httpx.Client(timeout=10.0) as client:
                response = client.post(url, json=payload)
                if response.status_code == 200:
                    data = response.json()
                    candidates = data.get("candidates", [])
                    if candidates and "content" in candidates[0]:
                        parts = candidates[0]["content"].get("parts", [])
                        if parts:
                            return parts[0].get("text", "")
                else:
                    logger.warning(f"Gemini API returned status {response.status_code}: {response.text}")
        except Exception as e:
            logger.error("Error communicating with Gemini API", exc_info=e)

        return None

    def generate_json(
        self,
        prompt: str,
        system_instruction: str = ""
    ) -> Optional[Dict[str, Any]]:
        if not settings.AI_ENABLED or not self.api_key:
            return None

        json_prompt = f"{prompt}\n\nIMPORTANT: Respond ONLY with a valid JSON object matching the requested fields."
        raw_text = self.generate_text(json_prompt, system_instruction)
        if not raw_text:
            return None

        try:
            # Strip markdown ```json codeblocks if returned
            clean_text = raw_text.strip()
            if clean_text.startswith("```json"):
                clean_text = clean_text[7:]
            if clean_text.startswith("```"):
                clean_text = clean_text[3:]
            if clean_text.endswith("```"):
                clean_text = clean_text[:-3]

            return json.loads(clean_text.strip())
        except Exception as e:
            logger.warning("Failed to parse JSON response from Gemini model", exc_info=e)
            return None

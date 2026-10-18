from functools import lru_cache
from typing import List

from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=True,
        extra="ignore",
    )

    APP_NAME: str = "Aggrey Shield"
    APP_VERSION: str = "0.1.0"
    ENVIRONMENT: str = "development"
    DEBUG: bool = True
    API_V1_STR: str = "/api/v1"

    DATABASE_URL: str = "postgresql+asyncpg://aggreyshield:changeme@localhost:5432/aggreyshield"

    CORS_ALLOWED_ORIGINS: str = "http://localhost:5173"

    LOG_LEVEL: str = "INFO"

    # --- Authentication ---
    JWT_SECRET_KEY: str
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30
    REFRESH_TOKEN_EXPIRE_DAYS: int = 30
    MFA_ISSUER_NAME: str = "AGGREY SHIELD"
    RESEND_API_KEY: str
    # Must be an address on a domain verified in Resend before real users
    # can receive email. onboarding@resend.dev only delivers to the owner
    # of the Resend account.
    EMAIL_FROM: str = "AGGREY SHIELD <onboarding@resend.dev>"
    # Enable ONLY behind exactly one trusted proxy (e.g. an AWS ALB) that
    # appends the real client IP to X-Forwarded-For. See core/rate_limit.py.
    TRUST_PROXY_HEADERS: bool = False
    PASSWORD_RESET_URL_BASE: str = "https://aggreykibe.blogspot.com/p/reset-password.html"
    @field_validator("ENVIRONMENT")
    @classmethod
    def validate_environment(cls, value: str) -> str:
        allowed = {"development", "staging", "production"}
        if value not in allowed:
            raise ValueError(
                f"ENVIRONMENT must be one of {sorted(allowed)}, got '{value}'"
            )
        return value

    @field_validator("LOG_LEVEL")
    @classmethod
    def validate_log_level(cls, value: str) -> str:
        allowed = {"DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"}
        normalized = value.upper()
        if normalized not in allowed:
            raise ValueError(
                f"LOG_LEVEL must be one of {allowed}, got '{value}'"
            )
        return normalized

    @field_validator("JWT_ALGORITHM")
    @classmethod
    def validate_jwt_algorithm(cls, value: str) -> str:
        allowed = {"HS256"}
        if value not in allowed:
            raise ValueError(
                f"JWT_ALGORITHM must be one of {sorted(allowed)}, got '{value}'"
            )
        return value

    @field_validator("ACCESS_TOKEN_EXPIRE_MINUTES")
    @classmethod
    def validate_access_token_expiry(cls, value: int) -> int:
        if value <= 0:
            raise ValueError("ACCESS_TOKEN_EXPIRE_MINUTES must be greater than 0")
        return value

    @field_validator("REFRESH_TOKEN_EXPIRE_DAYS")
    @classmethod
    def validate_refresh_token_expiry(cls, value: int) -> int:
        if value <= 0:
            raise ValueError("REFRESH_TOKEN_EXPIRE_DAYS must be greater than 0")
        return value

    @property
    def cors_origins_list(self) -> List[str]:
        return [
            origin.strip()
            for origin in self.CORS_ALLOWED_ORIGINS.split(",")
            if origin.strip()
        ]

    @property
    def is_production(self) -> bool:
        return self.ENVIRONMENT == "production"


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()

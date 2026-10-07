"""
Phase 1 automated tests.

These tests intentionally do not require PostgreSQL, Redis, or Docker to be
running — they verify the backend foundation (app boots, health endpoints
respond correctly, security headers are present, errors use the consistent
envelope) in isolation, exactly as required by Phase 1's scope.
"""


def test_liveness_check_returns_ok(client):
    response = client.get("/health")

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ok"
    assert body["app_name"] == "Aggrey Shield"


def test_readiness_check_returns_valid_shape(client):
    response = client.get("/api/v1/health")

    # 200 if PostgreSQL happens to be reachable in this environment,
    # 503 if it is not (expected in Phase 1, before Phase 3/12 provision it).
    assert response.status_code in (200, 503)

    body = response.json()
    assert body["status"] in ("ok", "degraded")
    assert body["database"] in ("connected", "unavailable")
    assert body["environment"] == "development"


def test_security_headers_are_present_on_every_response(client):
    response = client.get("/health")

    assert response.headers["X-Content-Type-Options"] == "nosniff"
    assert response.headers["X-Frame-Options"] == "DENY"
    assert response.headers["Referrer-Policy"] == "strict-origin-when-cross-origin"
    assert "Content-Security-Policy" in response.headers
    assert "X-Request-ID" in response.headers


def test_unknown_route_returns_consistent_error_envelope(client):
    response = client.get("/api/v1/this-route-does-not-exist")

    assert response.status_code == 404
    body = response.json()
    assert body["error"]["code"] == "NOT_FOUND"
    assert "message" in body["error"]
    assert body["error"]["details"] == []


def test_wrong_method_returns_405_with_error_envelope(client):
    response = client.post("/health")

    assert response.status_code == 405
    body = response.json()
    assert body["error"]["code"] == "METHOD_NOT_ALLOWED"


def test_openapi_docs_available_outside_production(client):
    response = client.get("/api/v1/openapi.json")

    assert response.status_code == 200
    assert response.json()["info"]["title"] == "Aggrey Shield"

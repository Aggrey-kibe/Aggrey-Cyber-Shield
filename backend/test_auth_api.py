import asyncio

import httpx


async def main():
    async with httpx.AsyncClient(
        base_url="http://127.0.0.1:8001"
    ) as client:
        response = await client.post(
            "/api/v1/auth/login",
            json={
                "email": "api-test@example.com",
                "password": "StrongPass123",
            },
        )

        print("Status:", response.status_code)
        print("Response:", response.text)

        if response.status_code == 200:
            data = response.json()
            print("Access token received:", bool(data.get("access_token")))
            print("Refresh token received:", bool(data.get("refresh_token")))


asyncio.run(main())
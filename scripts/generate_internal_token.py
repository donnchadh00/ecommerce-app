#!/usr/bin/env python3

import argparse
import base64
import hashlib
import hmac
import json
import time


def base64url(value: bytes) -> str:
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode("ascii")


def encode_segment(payload: dict) -> str:
    return base64url(json.dumps(payload, separators=(",", ":"), sort_keys=True).encode("utf-8"))


def build_token(secret: str, subject: str, user_id: int, expires_in_seconds: int) -> str:
    now = int(time.time())
    header = {"alg": "HS256", "typ": "JWT"}
    claims = {
        "sub": subject,
        "role": "INTERNAL",
        "roles": ["INTERNAL"],
        "userId": user_id,
        "iat": now,
        "exp": now + expires_in_seconds,
    }

    encoded_header = encode_segment(header)
    encoded_claims = encode_segment(claims)
    signing_input = f"{encoded_header}.{encoded_claims}".encode("ascii")
    signature = hmac.new(secret.encode("utf-8"), signing_input, hashlib.sha256).digest()
    return f"{encoded_header}.{encoded_claims}.{base64url(signature)}"


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Generate a PRODUCT_INTERNAL_TOKEN compatible with the shared JWT secret."
    )
    parser.add_argument("--secret", required=True, help="JWT_SECRET value")
    parser.add_argument("--subject", default="internal-product-client", help="JWT subject")
    parser.add_argument("--user-id", type=int, default=0, help="JWT userId claim")
    parser.add_argument(
        "--expires-days",
        type=int,
        default=365,
        help="Number of days until the token expires",
    )
    args = parser.parse_args()

    if len(args.secret.encode("utf-8")) < 32:
        raise SystemExit("JWT secret must be at least 32 bytes for HS256.")

    expires_in_seconds = args.expires_days * 24 * 60 * 60
    print(build_token(args.secret, args.subject, args.user_id, expires_in_seconds))


if __name__ == "__main__":
    main()

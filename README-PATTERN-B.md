# Authentication Architecture — Pattern B (platform default)

The platform separates three authorities. **No service ever stores or
transits end-user passwords.**

| Authority | Who | Holds |
|---|---|---|
| Token authority | Auth0 tenant | user credentials, token minting, JWKS |
| Profile authority | identity-service (this template) | the users/profiles database (source of truth for profile entities) |
| Verifiers | APIM gateway + every bound webservice | JWKS-based JWT validation (issuer + audience) |

## The flow

```
1. LOGIN (user → Auth0 directly; auth-code flow)
   browser ──redirect──▶ Auth0 Universal Login ──creds──▶ Auth0
           ◀─── token (with profile claims) ───┘
   The bound post-login Action copies app_metadata.profiles /
   active_profile into claims: https://cafe.io/profiles, active_profile.

2. CALL APIs (user → APIM → webservice)
   Authorization: Bearer <token>
   APIM validate-jwt (issuer/audience) ──▶ webservice re-verifies via JWKS

3. WHEN THE IDENTITY-SERVICE IS CALLED (never for login):
   a. Registration — creates profile entities (admin/user, Patient/...) in
      ITS postgres, creates/links the Auth0 user, syncs profiles to Auth0
      app_metadata via the Management API using ITS OWN client credentials
      (the "<component>-conn" secret - the only runtime credential it holds).
   b. Profile switching — POST /me/profiles/active {profile} → updates its
      DB + app_metadata → the user's NEXT token carries different claims.
      Same user, different active profile = different token contents.
   c. Profile data — tokens carry claim summaries only; services needing the
      rich profile rows call this service WITH the user's token.
```

## Secrets placement rules

- M2M client credentials (this service's own): Key Vault → ExternalSecret →
  k8s "<component>-conn" secret (needed at rest by the workload). ✅ k8s secret
- Seeded admin password: Key Vault ONLY (auth0-admin-password); fetched on
  demand (seed Job, smoke tests). ❌ never a k8s secret
- End-user passwords: Auth0 only. ❌ never stored by us anywhere

## Pattern A (ROPG) — not enabled

The deprecated alternative (user POSTs username/password to this service,
which forwards to Auth0 /oauth/token) is intentionally NOT enabled on the
tenant. If a first-party login form ever becomes a hard requirement
(CLI/native without browser), enable the password grant for ONE dedicated
client and add a /login proxy endpoint here — and document the MFA/social
capability loss that comes with it.

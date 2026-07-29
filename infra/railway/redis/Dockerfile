# Railway Redis service. Holds refresh-token revocation, AI budget counters
# (ai_budget_{exam,homework,chat}:{schoolId}:{yyyy-mm}) and rate-limit counters.
#
# maxmemory-policy MUST stay noeviction (the default, made explicit here): allkeys-lru could
# silently evict an AI budget counter mid-month, resetting a school's spend tracking. 48MB is
# far more than demo scale needs; if it ever fills, writes fail loudly instead of lying.
#
# Railway service settings: Root Directory = infra/railway/redis, volume at /data (appendonly
# persistence so budget/revocation state survives restarts).
# Required env: REDIS_PASSWORD (must match the backend's REDIS_PASSWORD).
FROM redis:8.8.0

COPY entrypoint.sh /academix-entrypoint.sh
ENTRYPOINT ["sh", "/academix-entrypoint.sh"]

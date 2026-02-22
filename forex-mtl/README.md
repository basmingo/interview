# Forex Service — Interview Submission Notes

This document is not a runbook. It is a concise explanation of the engineering decisions made in this solution, with emphasis on architecture, trade-offs, and rationale.

## Context
The task is to provide FX rates via HTTP while integrating with an external one-frame service under strict request limits.

The key constraints that shaped the design were:
- one-frame supports batch requests (`pair=...` repeated in query)
- one-frame may return business errors as HTTP `200` with body like `{"error":"..."}`
- rate freshness target is 5 minutes
- expected traffic allows a single-instance in-memory cache approach
- metrics supported on '/metrics' endpoint

## Key Principles
1. Preserve and extend the existing architecture rather than rewrite it.
2. Keep domain orchestration separate from infrastructure concerns.
3. Use Tagless Final style for boundaries and testability.
4. Prefer simple, explicit flows over abstract but hard-to-maintain patterns.
5. Make failure modes explicit and client-friendly.

## What Was Implemented
### 1) Layered architecture aligned with Tagless Final
New/updated modules are grouped by responsibility:
- `forex.services.rates` — rate service orchestration (`cache -> provider fallback`)
- `forex.services.provider` — one-frame HTTP client interpreter
- `forex.services.cache` — cache interpreters + periodic refresh job
- `forex.services.logging` — tagless logging algebra + interpreters

### 2) Cache-first strategy with periodic warm-up
- Added `CacheRefreshJob` that preloads all supported currency pairs every 4 minutes.
- Pair set is derived from `forex.domain.Currency` and materialized via `Rate.allPairs(...)`.
- One-frame is called in batch mode to reduce request volume and stay within external limits.

### 3) Provider contract designed for batch usage
- Provider algebra accepts `List[Rate.Pair]` and returns `Map[Rate.Pair, Rate]`.
- This removes positional assumptions and makes lookups deterministic.

### 4) API error wrapper and request validation
- Added structured API error response (`error_type`, `error_code`, `error_message`, `display_message`, `request_id`).
- Added safe currency parsing (`fromStringEither`) to avoid runtime `MatchError` on unsupported input.
- Validation now returns explicit client errors (for example: `UNSUPPORTED_CURRENCY`) instead of internal failures.

### 5) Smart and minimal logging
- Introduced `Logger[F]` algebra and interpreters (`slf4j`, `noop`).
- Logging is focused on meaningful events only:
  - provider lookup failures
  - malformed/invalid provider payloads
  - cache refresh result summary

## Architectural Decisions and Trade-offs
### Why in-memory Caffeine, not Redis
Given the stated traffic and freshness constraints, a local in-memory cache is the simplest reliable option.
- Pros: lower complexity, no network hop, faster reads, easier operational setup.
- Cons: no shared cache across instances.

For horizontal scale (multiple app instances), a shared cache (Redis) can be introduced later if consistency requirements tighten.

### Why validate currencies against the domain list
`forex.domain.Currency` defines the supported product scope.
- Pros: predictable API behavior, explicit product boundary, avoids accidental long-tail expansion.
- Cons: excludes provider-supported currencies outside current scope.

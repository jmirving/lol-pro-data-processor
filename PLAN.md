# lol-pro-data-processor Implementation Plan

Goal: consume raw Oracle's Elixir CSV handoff artifacts and publish normalized `all`, `players`, `teams`, and canonical one-row-per-game `drafts` CSVs using the repository's current documented/tested schemas.

## Assumptions
- The download cron publishes raw Oracle's Elixir CSVs per year and does not alter columns.
- Extra columns may appear in the input and are ignored where allowed by the current schema contract.
- Team rows are `participantid` 100/200; existing validation/filter behavior is defined by repository tests.
- Caller-provided artifact IDs may be used for deterministic output naming.
- Keep KISS/YAGNI; avoid new abstractions unless tests or current GitHub issues require them.

## Implemented baseline

1. Configuration and input discovery
   - Supports caller-owned input/output directories and optional explicit years.
   - Discovers expected Oracle's Elixir year files when years are omitted.

2. Input validation
   - Validates required headers and supports empty CSVs with valid headers.

3. Normalization and output splitting
   - Produces `all`, `players`, and `teams` outputs according to the repository's current schema/tests.

4. Multi-year merge behavior
   - Combines selected yearly inputs and fails clearly when required configured inputs are absent.

5. Atomic output publication
   - Writes temp files and safely replaces final artifacts.

6. Observability and exit codes
   - Reports processing counts and returns non-zero on validation or processing failures.

7. External command-adapter compatibility
   - Accepts standalone CLI aliases for caller-owned input/output paths.
   - Supports caller-selected deterministic artifact IDs.
   - Optionally emits the generic JSON result envelope on stdout.
   - Preserves direct standalone execution and keeps scheduling outside this repo.

8. Canonical draft artifact
   - Publishes the schema-versioned `drafts/drafts_<artifactId>.csv` contract.
   - Flattens complete Blue/Red team pairs into one deterministic row per game.
   - Excludes and reports incomplete or malformed draft games without changing
     the legacy output contracts.

## Current forward work

- Repository-local docs, tests, code, and GitHub issues are the source of truth for schema and behavior changes.
- Consumer migration to the canonical draft artifact remains separately scoped.

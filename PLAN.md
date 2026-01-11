# lol-pro-data-processor Implementation Plan

Goal: consume raw Oracle's Elixir CSV handoff artifacts (per-year files) and
publish normalized `all`, `players`, and `teams` CSVs with the DraftSage
minimal schema defined in `project-brain/DECISIONS.md`.

## Assumptions
- The download cron publishes raw Oracle's Elixir CSVs per year and does not
  alter columns.
- Extra columns may appear in the input; they are ignored (compatibility rule).
- Team rows are `participantid` 100/200; team rows must have pick1-5 populated.
- Processor writes versioned outputs in a single run directory with a run ID.
- Keep KISS/YAGNI; avoid new abstractions unless tests demand them.

## Plan (no implementation until approved)

1. Confirm contract + configuration surface
   - Define config keys for input dir, output dir, and explicit years list.
   - If years are omitted, discover `*_LoL_esports_match_data_from_OraclesElixir.csv`.
   - Document defaults and expected paths in README (or `CONFIG.md`).
   - Validation: config matches `project-brain/DECISIONS.md`.
   - Status: implemented (config + README + discovery).

2. Input discovery + header validation (TDD)
   - For each selected year file, read and validate required columns.
   - Accept empty CSVs if header is present.
   - Validation: unit tests cover missing headers and missing required columns.
   - Status: implemented (header validator + tests).

3. Normalize rows + split outputs (TDD)
   - Emit every row to `all`.
   - Emit `participantid` 100/200 rows to `teams`; everything else to `players`.
   - Drop team rows with missing picks (pick1-5) as specified by the contract.
   - Output column order matches the DraftSage schema exactly.
   - Validation: unit tests for team row filtering and output order.
   - Status: implemented (job + tests).

4. Multi-year merge behavior (TDD)
   - Combine rows across all selected year files into a single set of outputs.
   - If a configured year file is missing, fail fast with a clear error.
   - Validation: test multi-year merge and missing-file failure behavior.
   - Status: implemented (logic + tests).

5. Output writer + atomic publish (TDD)
   - Write each dataset to a temp file, then atomically move into place.
   - Ensure output layout:
     - `all/all_<runId>.csv`
     - `players/players_<runId>.csv`
     - `teams/teams_<runId>.csv`
   - Validation: integration test verifies atomic write and final layout.
   - Status: implemented (atomic publish + temp file cleanup test).

6. Observability + exit codes
   - Log per-file row counts and drop counts.
   - Non-zero exit on validation or IO failure.
   - Validation: tests cover error paths where feasible.
   - Status: per-file logging implemented; error-path tests still missing.

7. Documentation + usage examples
   - Update README with run instructions, config keys, and output layout.
   - Document the dependency on the download-cron handoff contract.
   - Validation: docs reflect current behavior and contract constraints.
   - Status: implemented (README).

## Notes
- Implementation will not begin until this plan is approved.

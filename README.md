# lol-pro-data-processor

Processing and normalization layer for Oracle's Elixir pro-game data, shared by
DraftSage and ChatLoL consumers.

## Scope
- Consume the raw Oracle's Elixir CSV handoff artifact
- Validate required columns and basic integrity
- Produce versioned normalized outputs for downstream services

## Out of scope
- Raw data acquisition
- Model training or inference
- UI or API serving

## Inputs/Outputs
- Input: Oracle's Elixir CSV per the Project Brain contract in `DECISIONS.md`.
- Output: Versioned normalized datasets (all/players/teams) in CSV format.

### Output schema (MVP)
Only the columns used by DraftSage are preserved, in this order:
```
gameid,league,split,year,date,game,patch,participantid,side,teamid,
ban1,ban2,ban3,ban4,ban5,pick1,pick2,pick3,pick4,pick5
```
Team rows (participantid 100/200) with missing picks are dropped. Non-team rows
are preserved even if picks are blank.

### Output layout (default)
```
build/prodata-processed/
  all/all_<artifactId>.csv
  players/players_<artifactId>.csv
  teams/teams_<artifactId>.csv
```

If no artifact ID is supplied, the standalone default is a UTC timestamp in
`yyyyMMdd_HHmmss` format.

## Run
```
gradle_safe bootRun
```

Build and run the standalone executable directly:
```bash
gradle_safe bootJar
java -jar build/libs/lol-pro-data-processor-1.0-SNAPSHOT.jar \
  --input-dir=/work/raw \
  --output-dir=/work/processed \
  --years=2025,2026
```

### Configuration
```
prodata.processor.input-dir=build/prodata
prodata.processor.output-dir=build/prodata-processed
prodata.processor.years=2024,2025
```
If `years` is empty, the processor scans `input-dir` for
`*_LoL_esports_match_data_from_OraclesElixir.csv`.

The executable also accepts these standalone CLI aliases:

- `--input-dir=<path>`: directory containing the per-year input CSVs
- `--output-dir=<path>`: destination root for normalized outputs
- `--years=<year,year>`: optional explicit input years
- `--artifact-id=<id>`: optional stable output suffix; rerunning the same ID
  atomically replaces those outputs
- `--structured-output=json`: emit the command-adapter JSON envelope on stdout

All options are non-interactive, and absolute or relative paths are accepted.
The existing `--prodata.processor.*` Spring properties remain supported for
standalone use.

### Command adapter contract

This repository remains an independently executable worker. A caller such as
`lol-data-refresh-cron` may invoke the JAR as a plain external command; this
worker does not own scheduling and contains no orchestration workflow logic.

The default stream/exit-code contract is:

- exit `0` after all required outputs are published
- exit non-zero on configuration, validation, or processing failure
- operational logs are written to stderr

With `--structured-output=json`, the entire stdout stream contains exactly one
JSON object (plus its terminating newline). The Spring banner and operational
logs are kept off stdout; operational logs continue on stderr. Successful
metadata includes the artifact ID, resolved input files, output paths, row
counts, and dropped incomplete-team-row count:

```json
{
  "status": "SUCCESS",
  "metadata": {
    "artifactId": "daily-2026-09-09",
    "inputFiles": ["/work/raw/2026_LoL_esports_match_data_from_OraclesElixir.csv"],
    "outputs": {
      "all": "/work/processed/all/all_daily-2026-09-09.csv",
      "players": "/work/processed/players/players_daily-2026-09-09.csv",
      "teams": "/work/processed/teams/teams_daily-2026-09-09.csv"
    },
    "rowCounts": {"all": 0, "players": 0, "teams": 0},
    "droppedTeamRows": 0
  }
}
```

Structured failures use status `FAILED`, reason code `PROCESSING_FAILED`, and
an `error` string. A non-zero process exit remains authoritative.

## Test
```
gradle_safe test
```

## Next steps
- Port additional processing logic from `draft-sage` if/when required by downstream consumers

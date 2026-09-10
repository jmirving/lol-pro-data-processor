# lol-pro-data-processor

Processing and normalization layer for Oracle's Elixir pro-game data, shared by
DraftSage and ChatLoL consumers.

## Scope
- Consume the raw Oracle's Elixir CSV handoff artifact
- Validate required columns and basic integrity
- Produce versioned normalized outputs and a canonical one-row-per-game draft artifact for downstream services

## Out of scope
- Raw data acquisition
- Model training or inference
- UI or API serving

## Inputs/Outputs
- Input: Oracle's Elixir per-year CSV exports.
- Output: Versioned normalized datasets (`all`, `players`, `teams`, and `drafts`) in CSV format.

### Output schema (MVP)
Only the columns used by DraftSage are preserved, in this order:
```
gameid,league,split,year,date,game,patch,participantid,side,teamid,
ban1,ban2,ban3,ban4,ban5,pick1,pick2,pick3,pick4,pick5,firstpick
```
Team rows (participantid 100/200) with missing picks are dropped. Non-team rows
are preserved even if picks are blank.

### Canonical draft schema (version 1)

`drafts` is the reusable DataGraph-facing contract. It contains exactly one row
for each complete game, with columns in this stable order:

```text
schema_version,gameid,date,year,split,league,patch,game,
blue_teamid,red_teamid,
blue_pick1,blue_pick2,blue_pick3,blue_pick4,blue_pick5,
red_pick1,red_pick2,red_pick3,red_pick4,red_pick5,
blue_ban1,blue_ban2,blue_ban3,blue_ban4,blue_ban5,
red_ban1,red_ban2,red_ban3,red_ban4,red_ban5
```

- `schema_version` is currently `1`. A breaking column or semantic change will
  increment it; additive changes to the other processor outputs do not.
- `gameid` is the source's stable game identity. Date, year, split, league,
  patch, and game number are retained as source context for historical filters.
- Team IDs and draft values are copied from the source's participant 100 (Blue)
  and participant 200 (Red) team rows. No scoring or model-derived fields are
  included.
- Rows are ordered lexicographically by `gameid`, so identical inputs produce
  identical CSV content when the same artifact ID is requested.
- A game is excluded if either team row is absent, a side/participant pairing is
  invalid, a side is duplicated, team context conflicts, a team ID is blank, or
  any pick or ban is blank. `droppedDraftGames` reports the excluded candidate
  count. These checks do not remove or otherwise change legacy outputs.

The caller-supplied `artifactId` is the artifact version: it appears in the
stable filename and in structured result metadata. Reusing an artifact ID
atomically replaces that named output. Structured metadata also exposes the
draft contract version as `schemaVersions.drafts`.

### Output layout (default)
```
build/prodata-processed/
  all/all_<artifactId>.csv
  players/players_<artifactId>.csv
  teams/teams_<artifactId>.csv
  drafts/drafts_<artifactId>.csv
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
    "schemaVersions": {"drafts": "1"},
    "inputFiles": ["/work/raw/2026_LoL_esports_match_data_from_OraclesElixir.csv"],
    "outputs": {
      "all": "/work/processed/all/all_daily-2026-09-09.csv",
      "players": "/work/processed/players/players_daily-2026-09-09.csv",
      "teams": "/work/processed/teams/teams_daily-2026-09-09.csv",
      "drafts": "/work/processed/drafts/drafts_daily-2026-09-09.csv"
    },
    "rowCounts": {"all": 0, "players": 0, "teams": 0, "drafts": 0},
    "droppedTeamRows": 0,
    "droppedDraftGames": 0
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

- Migrate consumers to the canonical draft artifact only through separately scoped changes.

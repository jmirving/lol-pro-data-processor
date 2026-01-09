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
  all/all_<runId>.csv
  players/players_<runId>.csv
  teams/teams_<runId>.csv
```

## Run
```
gradle_safe bootRun
```

### Configuration
```
prodata.processor.input-dir=build/prodata
prodata.processor.output-dir=build/prodata-processed
prodata.processor.years=2024,2025
```
If `years` is empty, the processor scans `input-dir` for
`*_LoL_esports_match_data_from_OraclesElixir.csv`.

## Test
```
gradle_safe test
```

## Next steps
- Port additional processing logic from `draft-sage` if/when required by downstream consumers

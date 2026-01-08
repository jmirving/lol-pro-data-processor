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
- Output: Versioned normalized datasets (all/players/teams) plus optional metadata.

## Next steps
- Implement schema validation and versioned output layout
- Port minimal processing logic from `draft-sage` once contracts are stable

package com.jmirving.prodata.processor.job;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.jmirving.prodata.processor.ProDataColumns;

final class DraftGameCollector {
    private static final List<String> CONTEXT_COLUMNS = List.of(
            "gameid", "date", "year", "split", "league", "patch", "game"
    );

    private final Map<String, DraftGame> games = new TreeMap<>();
    private long rowsWithoutGameId;

    void observe(List<String> values, boolean teamRow) {
        String gameId = value(values, "gameid");
        if (gameId.isBlank()) {
            rowsWithoutGameId++;
            return;
        }

        DraftGame game = games.computeIfAbsent(gameId, ignored -> new DraftGame());
        if (teamRow) {
            game.addTeam(values);
        }
    }

    Result finish() {
        List<List<String>> rows = new ArrayList<>();
        long droppedGames = rowsWithoutGameId;
        for (DraftGame game : games.values()) {
            List<String> row = game.toRow();
            if (row == null) {
                droppedGames++;
            } else {
                rows.add(row);
            }
        }
        return new Result(List.copyOf(rows), droppedGames);
    }

    private static String value(List<String> values, String column) {
        return values.get(ProDataColumns.OUTPUT_INDEX.get(column));
    }

    record Result(List<List<String>> rows, long droppedGames) {
    }

    private static final class DraftGame {
        private Map<String, String> context;
        private List<String> blue;
        private List<String> red;
        private boolean malformed;

        void addTeam(List<String> values) {
            Map<String, String> candidateContext = new LinkedHashMap<>();
            for (String column : CONTEXT_COLUMNS) {
                candidateContext.put(column, value(values, column));
            }
            if (context == null) {
                context = candidateContext;
            } else if (!context.equals(candidateContext)) {
                malformed = true;
            }

            String participantId = value(values, "participantid");
            String side = value(values, "side");
            if ("100".equals(participantId) && "blue".equalsIgnoreCase(side)) {
                if (blue != null) {
                    malformed = true;
                }
                blue = values;
            } else if ("200".equals(participantId) && "red".equalsIgnoreCase(side)) {
                if (red != null) {
                    malformed = true;
                }
                red = values;
            } else {
                malformed = true;
            }
        }

        List<String> toRow() {
            if (malformed || context == null || blue == null || red == null
                    || value(blue, "teamid").isBlank() || value(red, "teamid").isBlank()
                    || hasBlankDraftValue(blue) || hasBlankDraftValue(red)) {
                return null;
            }

            List<String> row = new ArrayList<>(ProDataColumns.DRAFT_COLUMNS.size());
            row.add(ProDataColumns.DRAFT_SCHEMA_VERSION);
            for (String column : CONTEXT_COLUMNS) {
                row.add(context.get(column));
            }
            row.add(value(blue, "teamid"));
            row.add(value(red, "teamid"));
            append(row, blue, ProDataColumns.PICK_COLUMNS);
            append(row, red, ProDataColumns.PICK_COLUMNS);
            append(row, blue, ProDataColumns.BAN_COLUMNS);
            append(row, red, ProDataColumns.BAN_COLUMNS);
            return List.copyOf(row);
        }

        private boolean hasBlankDraftValue(List<String> values) {
            return ProDataColumns.PICK_COLUMNS.stream().anyMatch(column -> value(values, column).isBlank())
                    || ProDataColumns.BAN_COLUMNS.stream().anyMatch(column -> value(values, column).isBlank());
        }

        private void append(List<String> row, List<String> values, List<String> columns) {
            for (String column : columns) {
                row.add(value(values, column));
            }
        }
    }
}

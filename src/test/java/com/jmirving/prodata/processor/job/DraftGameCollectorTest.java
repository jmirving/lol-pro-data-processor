package com.jmirving.prodata.processor.job;

import java.util.ArrayList;
import java.util.List;

import com.jmirving.prodata.processor.ProDataColumns;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DraftGameCollectorTest {

    @Test
    void producesOneDeterministicallyOrderedRowPerCompleteGame() {
        DraftGameCollector collector = new DraftGameCollector();
        collector.observe(team("game-b", "200", "Red", "red-b", "B"), true);
        collector.observe(team("game-a", "100", "Blue", "blue-a", "A"), true);
        collector.observe(team("game-b", "100", "Blue", "blue-b", "B"), true);
        collector.observe(team("game-a", "200", "Red", "red-a", "A"), true);

        DraftGameCollector.Result result = collector.finish();

        assertEquals(2, result.rows().size());
        assertEquals(0, result.droppedGames());
        assertEquals("game-a", result.rows().get(0).get(ProDataColumns.DRAFT_COLUMNS.indexOf("gameid")));
        assertEquals("blue-a", result.rows().get(0).get(ProDataColumns.DRAFT_COLUMNS.indexOf("blue_teamid")));
        assertEquals("red-a", result.rows().get(0).get(ProDataColumns.DRAFT_COLUMNS.indexOf("red_teamid")));
        assertEquals("A-blue-pick1", result.rows().get(0).get(ProDataColumns.DRAFT_COLUMNS.indexOf("blue_pick1")));
        assertEquals("A-red-ban5", result.rows().get(0).get(ProDataColumns.DRAFT_COLUMNS.indexOf("red_ban5")));
        assertEquals(ProDataColumns.DRAFT_SCHEMA_VERSION, result.rows().get(0).get(0));
    }

    @Test
    void dropsIncompleteAndMalformedGames() {
        DraftGameCollector collector = new DraftGameCollector();
        collector.observe(team("missing-red", "100", "Blue", "blue", "M"), true);

        List<String> missingBan = team("missing-ban", "100", "Blue", "blue", "N");
        missingBan.set(ProDataColumns.OUTPUT_INDEX.get("ban3"), "");
        collector.observe(missingBan, true);
        collector.observe(team("missing-ban", "200", "Red", "red", "N"), true);

        collector.observe(team("duplicate-blue", "100", "Blue", "blue", "D"), true);
        collector.observe(team("duplicate-blue", "100", "Blue", "blue-2", "D"), true);
        collector.observe(team("duplicate-blue", "200", "Red", "red", "D"), true);

        collector.observe(team("wrong-side", "100", "Red", "blue", "W"), true);
        collector.observe(team("wrong-side", "200", "Red", "red", "W"), true);

        assertEquals(0, collector.finish().rows().size());
        assertEquals(4, collector.finish().droppedGames());
    }

    private List<String> team(String gameId, String participantId, String side, String teamId, String marker) {
        List<String> values = new ArrayList<>();
        for (String column : ProDataColumns.OUTPUT_COLUMNS) {
            values.add(switch (column) {
                case "gameid" -> gameId;
                case "league" -> "LCK";
                case "split" -> "Summer";
                case "year" -> "2025";
                case "date" -> "2025-07-01";
                case "game" -> "1";
                case "patch" -> "15.13";
                case "participantid" -> participantId;
                case "side" -> side;
                case "teamid" -> teamId;
                case "ban1", "ban2", "ban3", "ban4", "ban5" ->
                        marker + "-" + side.toLowerCase() + "-" + column;
                case "pick1", "pick2", "pick3", "pick4", "pick5" ->
                        marker + "-" + side.toLowerCase() + "-" + column;
                default -> "";
            });
        }
        return values;
    }
}

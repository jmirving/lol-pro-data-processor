package com.jmirving.prodata.processor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ProDataColumns {
    public static final List<String> OUTPUT_COLUMNS = List.of(
            "gameid",
            "league",
            "split",
            "year",
            "date",
            "game",
            "patch",
            "participantid",
            "side",
            "teamid",
            "ban1",
            "ban2",
            "ban3",
            "ban4",
            "ban5",
            "pick1",
            "pick2",
            "pick3",
            "pick4",
            "pick5",
            "firstpick"
    );
    public static final Set<String> REQUIRED_COLUMNS = Set.of(
            "gameid",
            "league",
            "split",
            "year",
            "date",
            "game",
            "patch",
            "participantid",
            "side",
            "teamid",
            "ban1",
            "ban2",
            "ban3",
            "ban4",
            "ban5",
            "pick1",
            "pick2",
            "pick3",
            "pick4",
            "pick5"
    );
    public static final List<String> PICK_COLUMNS = List.of("pick1", "pick2", "pick3", "pick4", "pick5");
    public static final List<String> BAN_COLUMNS = List.of("ban1", "ban2", "ban3", "ban4", "ban5");
    public static final String DRAFT_SCHEMA_VERSION = "1";
    public static final List<String> DRAFT_COLUMNS = List.of(
            "schema_version",
            "gameid",
            "date",
            "year",
            "split",
            "league",
            "patch",
            "game",
            "blue_teamid",
            "red_teamid",
            "blue_pick1",
            "blue_pick2",
            "blue_pick3",
            "blue_pick4",
            "blue_pick5",
            "red_pick1",
            "red_pick2",
            "red_pick3",
            "red_pick4",
            "red_pick5",
            "blue_ban1",
            "blue_ban2",
            "blue_ban3",
            "blue_ban4",
            "blue_ban5",
            "red_ban1",
            "red_ban2",
            "red_ban3",
            "red_ban4",
            "red_ban5"
    );
    public static final Map<String, Integer> OUTPUT_INDEX = buildOutputIndex();

    private ProDataColumns() {
    }

    private static Map<String, Integer> buildOutputIndex() {
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < OUTPUT_COLUMNS.size(); i++) {
            index.put(OUTPUT_COLUMNS.get(i), i);
        }
        return Collections.unmodifiableMap(index);
    }
}

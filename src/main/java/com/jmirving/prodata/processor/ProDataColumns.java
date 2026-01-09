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
            "pick5"
    );
    public static final Set<String> REQUIRED_COLUMNS = Set.copyOf(OUTPUT_COLUMNS);
    public static final List<String> PICK_COLUMNS = List.of("pick1", "pick2", "pick3", "pick4", "pick5");
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

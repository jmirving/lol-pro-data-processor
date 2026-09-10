package com.jmirving.prodata.processor.job;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record ProDataProcessingResult(
        String artifactId,
        List<Path> inputFiles,
        Map<String, Path> outputs,
        Map<String, Long> rowCounts,
        long droppedTeamRows,
        long droppedDraftGames
) {
}

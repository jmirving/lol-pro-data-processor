package com.jmirving.prodata.processor.job;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jmirving.prodata.processor.ProDataColumns;

final class WorkerResultWriter {
    private final ObjectMapper objectMapper;
    private final PrintStream stdout;

    WorkerResultWriter(ObjectMapper objectMapper, PrintStream stdout) {
        this.objectMapper = objectMapper;
        this.stdout = stdout;
    }

    void writeSuccess(ProDataProcessingResult result) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("artifactId", result.artifactId());
        metadata.put("schemaVersions", Map.of("drafts", ProDataColumns.DRAFT_SCHEMA_VERSION));
        metadata.put("inputFiles", stringifyPaths(result.inputFiles()));
        metadata.put("outputs", stringifyPaths(result.outputs()));
        metadata.put("rowCounts", result.rowCounts());
        metadata.put("droppedTeamRows", result.droppedTeamRows());
        metadata.put("droppedDraftGames", result.droppedDraftGames());

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("status", "SUCCESS");
        envelope.put("metadata", metadata);
        write(envelope);
    }

    void writeFailure(Exception exception) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("status", "FAILED");
        envelope.put("reasonCode", "PROCESSING_FAILED");
        envelope.put("error", errorMessage(exception));
        write(envelope);
    }

    private List<String> stringifyPaths(List<Path> paths) {
        return paths.stream().map(Path::toString).toList();
    }

    private Map<String, String> stringifyPaths(Map<String, Path> paths) {
        Map<String, String> values = new LinkedHashMap<>();
        paths.forEach((name, path) -> values.put(name, path.toString()));
        return values;
    }

    private String errorMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private void write(Map<String, Object> envelope) {
        try {
            stdout.println(objectMapper.writeValueAsString(envelope));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize structured worker result", e);
        }
    }
}

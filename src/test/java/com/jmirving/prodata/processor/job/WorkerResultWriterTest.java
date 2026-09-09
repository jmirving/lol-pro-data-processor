package com.jmirving.prodata.processor.job;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WorkerResultWriterTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void writesGenericSuccessEnvelopeWithWorkerMetadata() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        WorkerResultWriter writer = new WorkerResultWriter(objectMapper, new PrintStream(bytes, true, StandardCharsets.UTF_8));
        ProDataProcessingResult result = new ProDataProcessingResult(
                "run-42",
                List.of(Path.of("/tmp/raw/2025.csv")),
                Map.of("all", Path.of("/tmp/out/all/all_run-42.csv")),
                Map.of("all", 12L, "players", 10L, "teams", 2L),
                1L
        );

        writer.writeSuccess(result);

        JsonNode envelope = objectMapper.readTree(bytes.toString(StandardCharsets.UTF_8));
        assertEquals("SUCCESS", envelope.get("status").asText());
        assertFalse(envelope.has("reasonCode"));
        assertEquals("run-42", envelope.at("/metadata/artifactId").asText());
        assertEquals(12, envelope.at("/metadata/rowCounts/all").asLong());
        assertEquals("/tmp/out/all/all_run-42.csv", envelope.at("/metadata/outputs/all").asText());
    }

    @Test
    void writesGenericFailureEnvelope() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        WorkerResultWriter writer = new WorkerResultWriter(objectMapper, new PrintStream(bytes, true, StandardCharsets.UTF_8));

        writer.writeFailure(new IllegalStateException("missing input"));

        JsonNode envelope = objectMapper.readTree(bytes.toString(StandardCharsets.UTF_8));
        assertEquals("FAILED", envelope.get("status").asText());
        assertEquals("PROCESSING_FAILED", envelope.get("reasonCode").asText());
        assertEquals("missing input", envelope.get("error").asText());
    }
}

package com.jmirving.prodata.processor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkerExecutableIntegrationTest {
    private static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(30);
    private static final String INPUT_FILE_NAME =
            "2025_LoL_esports_match_data_from_OraclesElixir.csv";

    private final ObjectMapper strictObjectMapper = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    @TempDir
    Path tempDir;

    @Test
    void structuredSuccessReservesStdoutForExactlyOneJsonEnvelope() throws Exception {
        Path inputDir = prepareInput("structured-success");
        Path outputDir = tempDir.resolve("structured-success-output");

        ProcessResult process = runWorker(List.of(
                "--input-dir=" + inputDir,
                "--output-dir=" + outputDir,
                "--years=2025",
                "--artifact-id=integration-success",
                "--structured-output=json",
                "--spring.main.banner-mode=console"
        ));

        assertEquals(0, process.exitCode(), process.stderr());
        JsonNode envelope = strictObjectMapper.readTree(process.stdout());
        assertEquals("SUCCESS", envelope.path("status").asText());
        assertEquals("integration-success", envelope.at("/metadata/artifactId").asText());
        assertEquals(2, envelope.at("/metadata/rowCounts/all").asLong());
        assertEquals(1, envelope.at("/metadata/rowCounts/players").asLong());
        assertEquals(1, envelope.at("/metadata/rowCounts/teams").asLong());
        assertEquals(0, envelope.at("/metadata/droppedTeamRows").asLong());
        assertTrue(envelope.at("/metadata/inputFiles/0").asText().endsWith(INPUT_FILE_NAME));
        assertFalse(process.stdout().contains("Spring"));
        assertFalse(process.stdout().contains("Processing"));
        assertTrue(process.stderr().contains("Processing"), process.stderr());
        assertTrue(Files.isRegularFile(outputDir.resolve("all/all_integration-success.csv")));
    }

    @Test
    void structuredFailureIsParseableAndExitsNonZero() throws Exception {
        Path inputDir = Files.createDirectories(tempDir.resolve("structured-failure-input"));

        ProcessResult process = runWorker(List.of(
                "--input-dir=" + inputDir,
                "--output-dir=" + tempDir.resolve("structured-failure-output"),
                "--years=2025",
                "--artifact-id=integration-failure",
                "--structured-output=json"
        ));

        assertNotEquals(0, process.exitCode());
        JsonNode envelope = strictObjectMapper.readTree(process.stdout());
        assertEquals("FAILED", envelope.path("status").asText());
        assertEquals("PROCESSING_FAILED", envelope.path("reasonCode").asText());
        assertTrue(envelope.path("error").asText().contains("Missing input CSV for year 2025"));
        assertFalse(process.stdout().contains("Spring"));
        assertTrue(process.stderr().contains("Pro data processing failed"), process.stderr());
    }

    @Test
    void plainStandaloneExecutionStillSucceeds() throws Exception {
        Path inputDir = prepareInput("plain-success");
        Path outputDir = tempDir.resolve("plain-success-output");

        ProcessResult process = runWorker(List.of(
                "--input-dir=" + inputDir,
                "--output-dir=" + outputDir,
                "--years=2025",
                "--artifact-id=plain-success"
        ));

        assertEquals(0, process.exitCode(), process.stderr());
        assertTrue(process.stdout().isBlank(), process.stdout());
        assertTrue(process.stderr().contains("Processing"), process.stderr());
        assertTrue(Files.isRegularFile(outputDir.resolve("all/all_plain-success.csv")));
        assertTrue(Files.isRegularFile(outputDir.resolve("players/players_plain-success.csv")));
        assertTrue(Files.isRegularFile(outputDir.resolve("teams/teams_plain-success.csv")));
    }

    private Path prepareInput(String name) throws IOException {
        Path inputDir = Files.createDirectories(tempDir.resolve(name + "-input"));
        try (var input = getClass().getResourceAsStream("/oracle-elixir/" + INPUT_FILE_NAME)) {
            if (input == null) {
                throw new IllegalStateException("Missing test resource " + INPUT_FILE_NAME);
            }
            Files.copy(input, inputDir.resolve(INPUT_FILE_NAME));
        }
        return inputDir;
    }

    private ProcessResult runWorker(List<String> arguments) throws Exception {
        Path jar = Path.of(System.getProperty("workerJar"));
        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        ProcessBuilder builder = new ProcessBuilder();
        builder.command().add(java.toString());
        builder.command().add("-jar");
        builder.command().add(jar.toString());
        builder.command().addAll(arguments);

        Process process = builder.start();
        CompletableFuture<String> stdout = readAsync(process.getInputStream());
        CompletableFuture<String> stderr = readAsync(process.getErrorStream());
        assertTrue(process.waitFor(PROCESS_TIMEOUT.toSeconds(), TimeUnit.SECONDS), "Worker process timed out");
        return new ProcessResult(process.exitValue(), stdout.get(), stderr.get());
    }

    private CompletableFuture<String> readAsync(java.io.InputStream stream) {
        return CompletableFuture.supplyAsync(() -> {
            try (stream) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Could not read worker process stream", e);
            }
        });
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {
    }
}

package com.jmirving.prodata.processor.job;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jmirving.prodata.processor.ProDataColumns;
import com.jmirving.prodata.processor.config.ProDataProcessorProperties;
import com.jmirving.prodata.processor.validate.CsvHeaderValidator;
import com.jmirving.prodata.processor.validate.CsvHeaderValidator.HeaderIndex;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProDataProcessorJob {
    private static final Logger logger = LoggerFactory.getLogger(ProDataProcessorJob.class);
    private static final Pattern FILE_PATTERN =
            Pattern.compile("(?<year>\\d{4})_LoL_esports_match_data_from_OraclesElixir\\.csv");

    private final ProDataProcessorProperties properties;
    private final CsvHeaderValidator headerValidator;

    public ProDataProcessorJob(
            ProDataProcessorProperties properties,
            CsvHeaderValidator headerValidator
    ) {
        this.properties = properties;
        this.headerValidator = headerValidator;
    }

    public int run() {
        try {
            execute();
            return 0;
        } catch (Exception e) {
            logger.error("Pro data processing failed", e);
            return 1;
        }
    }

    void execute() throws IOException {
        Path inputDir = resolveInputDir();
        Path outputDir = resolveOutputDir();
        List<Path> inputFiles = resolveInputFiles(inputDir);
        String runId = runId();

        Path allOutput = outputDir.resolve("all").resolve("all_" + runId + ".csv");
        Path playersOutput = outputDir.resolve("players").resolve("players_" + runId + ".csv");
        Path teamsOutput = outputDir.resolve("teams").resolve("teams_" + runId + ".csv");

        Files.createDirectories(allOutput.getParent());
        Files.createDirectories(playersOutput.getParent());
        Files.createDirectories(teamsOutput.getParent());

        Path allTemp = createTempFile(allOutput.getParent(), "tmp_all_");
        Path playersTemp = createTempFile(playersOutput.getParent(), "tmp_players_");
        Path teamsTemp = createTempFile(teamsOutput.getParent(), "tmp_teams_");

        long allCount = 0;
        long playerCount = 0;
        long teamCount = 0;
        long droppedTeamCount = 0;

        try {
            try (BufferedWriter allWriter = Files.newBufferedWriter(allTemp);
                 BufferedWriter playersWriter = Files.newBufferedWriter(playersTemp);
                 BufferedWriter teamsWriter = Files.newBufferedWriter(teamsTemp);
                 CSVPrinter allPrinter = new CSVPrinter(allWriter, CSVFormat.DEFAULT.withHeader(headerArray()));
                 CSVPrinter playersPrinter = new CSVPrinter(playersWriter, CSVFormat.DEFAULT.withHeader(headerArray()));
                 CSVPrinter teamsPrinter = new CSVPrinter(teamsWriter, CSVFormat.DEFAULT.withHeader(headerArray()))
            ) {
                for (Path inputFile : inputFiles) {
                    logger.info("Processing {}", inputFile);
                    long fileAllCount = 0;
                    long filePlayerCount = 0;
                    long fileTeamCount = 0;
                    long fileDroppedTeamCount = 0;
                    try (BufferedReader reader = Files.newBufferedReader(inputFile)) {
                        String headerLine = reader.readLine();
                        HeaderIndex headerIndex = headerValidator.validate(headerLine);
                        try (CSVParser parser = CSVParser.parse(reader, CSVFormat.DEFAULT)) {
                            for (CSVRecord record : parser) {
                                List<String> values = buildValues(record, headerIndex);
                                RowFlags flags = classifyRow(values);
                                if (flags.isTeam() && hasMissingPick(values)) {
                                    fileDroppedTeamCount++;
                                    droppedTeamCount++;
                                    continue;
                                }
                                allPrinter.printRecord(values);
                                allCount++;
                                fileAllCount++;
                                if (flags.isPlayer()) {
                                    playersPrinter.printRecord(values);
                                    playerCount++;
                                    filePlayerCount++;
                                }
                                if (flags.isTeam()) {
                                    teamsPrinter.printRecord(values);
                                    teamCount++;
                                    fileTeamCount++;
                                }
                            }
                        }
                    }
                    logger.info(
                            "Processed {} (all={}, players={}, teams={}, droppedTeamRows={})",
                            inputFile.getFileName(),
                            fileAllCount,
                            filePlayerCount,
                            fileTeamCount,
                            fileDroppedTeamCount
                    );
                }
            }
            moveAtomic(allTemp, allOutput);
            moveAtomic(playersTemp, playersOutput);
            moveAtomic(teamsTemp, teamsOutput);
        } catch (Exception e) {
            deleteIfExists(allTemp);
            deleteIfExists(playersTemp);
            deleteIfExists(teamsTemp);
            throw e;
        }

        logger.info(
                "Pro data processing complete (all={}, players={}, teams={}, droppedTeamRows={}) -> {}",
                allCount,
                playerCount,
                teamCount,
                droppedTeamCount,
                outputDir
        );
    }

    private Path resolveInputDir() {
        String inputDir = properties.getInputDir();
        String resolved = (inputDir == null || inputDir.isBlank()) ? "build/prodata" : inputDir;
        return Paths.get(resolved).toAbsolutePath();
    }

    private Path resolveOutputDir() {
        String outputDir = properties.getOutputDir();
        String resolved = (outputDir == null || outputDir.isBlank()) ? "build/prodata-processed" : outputDir;
        return Paths.get(resolved).toAbsolutePath();
    }

    private List<Path> resolveInputFiles(Path inputDir) throws IOException {
        List<Integer> configuredYears = properties.getYears();
        if (configuredYears == null) {
            configuredYears = List.of();
        }
        if (!configuredYears.isEmpty()) {
            List<Path> resolved = new ArrayList<>();
            for (Integer year : configuredYears) {
                if (year == null) {
                    continue;
                }
                Path candidate = inputDir.resolve(fileNameForYear(year));
                if (!Files.exists(candidate)) {
                    throw new IllegalStateException("Missing input CSV for year " + year + ": " + candidate);
                }
                resolved.add(candidate);
            }
            return resolved;
        }

        if (!Files.exists(inputDir)) {
            throw new IllegalStateException("Input directory does not exist: " + inputDir);
        }
        try (var stream = Files.list(inputDir)) {
            List<Path> matches = stream
                    .filter(path -> FILE_PATTERN.matcher(path.getFileName().toString()).matches())
                    .toList();
            if (matches.isEmpty()) {
                throw new IllegalStateException("No Oracle's Elixir CSVs found in " + inputDir);
            }
            return matches.stream()
                    .sorted(Comparator.comparingInt(this::extractYear).thenComparing(Path::toString))
                    .toList();
        }
    }

    private String fileNameForYear(int year) {
        return String.format(Locale.ROOT, "%d_LoL_esports_match_data_from_OraclesElixir.csv", year);
    }

    private int extractYear(Path path) {
        Matcher matcher = FILE_PATTERN.matcher(path.getFileName().toString());
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group("year"));
        }
        return Integer.MAX_VALUE;
    }

    private String runId() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneOffset.UTC);
        return formatter.format(Instant.now());
    }

    private List<String> buildValues(CSVRecord record, HeaderIndex headerIndex) {
        List<String> values = new ArrayList<>(ProDataColumns.OUTPUT_COLUMNS.size());
        for (String column : ProDataColumns.OUTPUT_COLUMNS) {
            values.add(readValue(record, headerIndex, column));
        }
        return values;
    }

    private RowFlags classifyRow(List<String> values) {
        String participantText = values.get(ProDataColumns.OUTPUT_INDEX.get("participantid"));
        OptionalInt participantId = parseInt(participantText);
        boolean isTeam = participantId.isPresent() && (participantId.getAsInt() == 100 || participantId.getAsInt() == 200);
        boolean isPlayer = participantId.isPresent() && participantId.getAsInt() >= 1 && participantId.getAsInt() <= 10;
        return new RowFlags(isPlayer, isTeam);
    }

    private boolean hasMissingPick(List<String> values) {
        for (String column : ProDataColumns.PICK_COLUMNS) {
            Integer index = ProDataColumns.OUTPUT_INDEX.get(column);
            if (index == null) {
                continue;
            }
            String value = values.get(index);
            if (value == null || value.isBlank()) {
                return true;
            }
        }
        return false;
    }

    private String readValue(CSVRecord record, HeaderIndex headerIndex, String column) {
        int index = headerIndex.indexOf(column);
        if (index < 0 || index >= record.size()) {
            return "";
        }
        String value = record.get(index);
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private OptionalInt parseInt(String value) {
        if (value == null || value.isBlank()) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    private String[] headerArray() {
        return ProDataColumns.OUTPUT_COLUMNS.toArray(new String[0]);
    }

    private Path createTempFile(Path dir, String prefix) throws IOException {
        return Files.createTempFile(dir, prefix, ".csv");
    }

    private void moveAtomic(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IOException("Atomic move failed for " + target, e);
        }
    }

    private void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            logger.warn("Failed to delete temp file {}", path, e);
        }
    }

    private record RowFlags(boolean isPlayer, boolean isTeam) {
    }
}

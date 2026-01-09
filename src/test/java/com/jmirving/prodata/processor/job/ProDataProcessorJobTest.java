package com.jmirving.prodata.processor.job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.jmirving.prodata.processor.ProDataColumns;
import com.jmirving.prodata.processor.config.ProDataProcessorProperties;
import com.jmirving.prodata.processor.validate.CsvHeaderValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProDataProcessorJobTest {

    @TempDir
    Path tempDir;

    @Test
    void writesAllPlayersTeamsWithFilteredColumns() throws IOException {
        Path inputDir = tempDir.resolve("input");
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(inputDir);

        Path inputFile = inputDir.resolve("2025_LoL_esports_match_data_from_OraclesElixir.csv");
        Files.writeString(inputFile, buildInputCsv());

        ProDataProcessorProperties properties = new ProDataProcessorProperties();
        properties.setInputDir(inputDir.toString());
        properties.setOutputDir(outputDir.toString());
        properties.setYears(List.of(2025));

        ProDataProcessorJob job = new ProDataProcessorJob(properties, new CsvHeaderValidator());
        job.execute();

        Path allOutput = firstCsv(outputDir.resolve("all"));
        Path playersOutput = firstCsv(outputDir.resolve("players"));
        Path teamsOutput = firstCsv(outputDir.resolve("teams"));

        List<String> allLines = Files.readAllLines(allOutput);
        List<String> playerLines = Files.readAllLines(playersOutput);
        List<String> teamLines = Files.readAllLines(teamsOutput);

        String header = String.join(",", ProDataColumns.OUTPUT_COLUMNS);
        assertEquals(header, allLines.get(0));
        assertEquals(header, playerLines.get(0));
        assertEquals(header, teamLines.get(0));

        assertEquals(3, allLines.size());
        assertEquals(2, playerLines.size());
        assertEquals(2, teamLines.size());
    }

    private Path firstCsv(Path dir) throws IOException {
        try (var stream = Files.list(dir)) {
            return stream.filter(path -> path.getFileName().toString().endsWith(".csv"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No CSV output in " + dir));
        }
    }

    private String buildInputCsv() {
        String header = String.join(",", ProDataColumns.OUTPUT_COLUMNS);
        String teamMissingPick = String.join(",",
                "1",
                "LCS",
                "Spring",
                "2025",
                "2025-01-01 10:00:00",
                "1",
                "13.1",
                "100",
                "Blue",
                "10",
                "BAN1",
                "BAN2",
                "BAN3",
                "BAN4",
                "BAN5",
                "PICK1",
                "",
                "PICK3",
                "PICK4",
                "PICK5"
        );
        String teamComplete = String.join(",",
                "2",
                "LCS",
                "Spring",
                "2025",
                "2025-01-01 10:00:00",
                "1",
                "13.1",
                "200",
                "Red",
                "20",
                "BAN1",
                "BAN2",
                "BAN3",
                "BAN4",
                "BAN5",
                "PICK1",
                "PICK2",
                "PICK3",
                "PICK4",
                "PICK5"
        );
        String playerRow = String.join(",",
                "3",
                "LCS",
                "Spring",
                "2025",
                "2025-01-01 10:00:00",
                "1",
                "13.1",
                "1",
                "Blue",
                "10",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                ""
        );
        return String.join(System.lineSeparator(), header, teamMissingPick, teamComplete, playerRow);
    }
}

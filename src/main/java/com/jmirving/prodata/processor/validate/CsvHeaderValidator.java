package com.jmirving.prodata.processor.validate;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.jmirving.prodata.processor.ProDataColumns;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.springframework.stereotype.Component;

@Component
public class CsvHeaderValidator {

    public HeaderIndex validate(Path csvPath) throws IOException {
        if (csvPath == null || !Files.exists(csvPath)) {
            throw new CsvValidationException("CSV path does not exist");
        }
        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            String headerLine = reader.readLine();
            return validate(headerLine);
        }
    }

    public HeaderIndex validate(String headerLine) throws IOException {
        if (headerLine == null || headerLine.isBlank()) {
            throw new CsvValidationException("CSV header row is missing");
        }
        headerLine = stripBom(headerLine);
        List<String> headers = parseHeader(headerLine);
        Set<String> normalized = normalizeHeaders(headers);
        List<String> missing = findMissing(normalized);
        if (!missing.isEmpty()) {
            throw new CsvValidationException("Missing required columns: " + String.join(", ", missing));
        }
        return new HeaderIndex(headers, buildIndex(headers));
    }

    private List<String> parseHeader(String headerLine) throws IOException {
        try (CSVParser parser = CSVParser.parse(headerLine, CSVFormat.DEFAULT)) {
            var iterator = parser.iterator();
            if (!iterator.hasNext()) {
                return List.of();
            }
            List<String> headers = new ArrayList<>();
            for (String value : iterator.next()) {
                headers.add(value);
            }
            return headers;
        }
    }

    private Set<String> normalizeHeaders(List<String> headers) {
        Set<String> normalized = new HashSet<>();
        for (String header : headers) {
            if (header != null) {
                normalized.add(normalize(header));
            }
        }
        return normalized;
    }

    private List<String> findMissing(Set<String> normalizedHeaders) {
        List<String> missing = new ArrayList<>();
        for (String required : ProDataColumns.REQUIRED_COLUMNS) {
            if (!normalizedHeaders.contains(required)) {
                missing.add(required);
            }
        }
        return missing;
    }

    private Map<String, Integer> buildIndex(List<String> headers) {
        Map<String, Integer> indexByName = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            if (header != null) {
                indexByName.put(normalize(header), i);
            }
        }
        return indexByName;
    }

    private String normalize(String header) {
        return header.trim().toLowerCase(Locale.ROOT);
    }

    private String stripBom(String headerLine) {
        if (headerLine.startsWith("\uFEFF")) {
            return headerLine.substring(1);
        }
        return headerLine;
    }

    public static final class HeaderIndex {
        private final List<String> headers;
        private final Map<String, Integer> indexByName;

        public HeaderIndex(List<String> headers, Map<String, Integer> indexByName) {
            this.headers = List.copyOf(headers);
            this.indexByName = Map.copyOf(indexByName);
        }

        public int indexOf(String column) {
            Integer index = indexByName.get(column);
            return index == null ? -1 : index;
        }

        public List<String> headers() {
            return headers;
        }
    }
}

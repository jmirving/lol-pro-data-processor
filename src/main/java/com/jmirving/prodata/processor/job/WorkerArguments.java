package com.jmirving.prodata.processor.job;

import java.util.Arrays;
import java.util.List;

import com.jmirving.prodata.processor.config.ProDataProcessorProperties;
import org.springframework.boot.ApplicationArguments;

final class WorkerArguments {
    private WorkerArguments() {
    }

    static void apply(ApplicationArguments arguments, ProDataProcessorProperties properties) {
        option(arguments, "structured-output").ifPresent(properties::setStructuredOutput);
        option(arguments, "input-dir").ifPresent(properties::setInputDir);
        option(arguments, "output-dir").ifPresent(properties::setOutputDir);
        option(arguments, "artifact-id").ifPresent(properties::setArtifactId);
        option(arguments, "years").ifPresent(value -> properties.setYears(parseYears(value)));
    }

    private static java.util.Optional<String> option(ApplicationArguments arguments, String name) {
        if (!arguments.containsOption(name)) {
            return java.util.Optional.empty();
        }
        List<String> values = arguments.getOptionValues(name);
        if (values == null || values.size() != 1 || values.get(0).isBlank()) {
            throw new IllegalArgumentException("--" + name + " requires exactly one non-blank value");
        }
        return java.util.Optional.of(values.get(0));
    }

    private static List<Integer> parseYears(String value) {
        try {
            return Arrays.stream(value.split(",", -1))
                    .map(String::trim)
                    .peek(year -> {
                        if (year.isEmpty()) {
                            throw new IllegalArgumentException("Year values must not be blank");
                        }
                    })
                    .map(Integer::valueOf)
                    .toList();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--years must be a comma-delimited list of integers", e);
        }
    }
}

package com.jmirving.prodata.processor.job;

import java.util.List;

import com.jmirving.prodata.processor.config.ProDataProcessorProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkerArgumentsTest {

    @Test
    void appliesStandaloneCliAliases() {
        ProDataProcessorProperties properties = new ProDataProcessorProperties();
        DefaultApplicationArguments arguments = new DefaultApplicationArguments(
                "--input-dir=/tmp/raw",
                "--output-dir=/tmp/processed",
                "--years=2025,2026",
                "--artifact-id=run-42",
                "--structured-output=json"
        );

        WorkerArguments.apply(arguments, properties);

        assertEquals("/tmp/raw", properties.getInputDir());
        assertEquals("/tmp/processed", properties.getOutputDir());
        assertEquals(List.of(2025, 2026), properties.getYears());
        assertEquals("run-42", properties.getArtifactId());
        assertEquals("json", properties.getStructuredOutput());
    }
}

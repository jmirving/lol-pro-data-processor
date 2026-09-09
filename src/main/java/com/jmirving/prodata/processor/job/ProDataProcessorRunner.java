package com.jmirving.prodata.processor.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jmirving.prodata.processor.config.ProDataProcessorProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ProDataProcessorRunner implements ApplicationRunner {
    private final ProDataProcessorJob job;
    private final ProDataProcessorProperties properties;

    public ProDataProcessorRunner(ProDataProcessorJob job, ProDataProcessorProperties properties) {
        this.job = job;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        WorkerResultWriter resultWriter = new WorkerResultWriter(new ObjectMapper(), System.out);
        try {
            WorkerArguments.apply(args, properties);
            boolean structured = structuredOutputEnabled();
            ProDataProcessingResult result = job.run();
            if (structured) {
                resultWriter.writeSuccess(result);
            }
            System.exit(0);
        } catch (Exception e) {
            if ("json".equalsIgnoreCase(properties.getStructuredOutput())) {
                resultWriter.writeFailure(e);
            }
            System.exit(1);
        }
    }

    private boolean structuredOutputEnabled() {
        String value = properties.getStructuredOutput();
        if (value == null || value.isBlank()) {
            return false;
        }
        if (!"json".equalsIgnoreCase(value)) {
            throw new IllegalArgumentException("structured-output must be 'json' when provided");
        }
        return true;
    }
}

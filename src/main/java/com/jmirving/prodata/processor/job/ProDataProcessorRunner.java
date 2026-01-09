package com.jmirving.prodata.processor.job;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ProDataProcessorRunner implements ApplicationRunner {
    private final ProDataProcessorJob job;

    public ProDataProcessorRunner(ProDataProcessorJob job) {
        this.job = job;
    }

    @Override
    public void run(ApplicationArguments args) {
        int exitCode = job.run();
        System.exit(exitCode);
    }
}

package com.jmirving.prodata.processor;

import com.jmirving.prodata.processor.job.StructuredOutputStreams;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ProDataProcessorApplication {
    public static void main(String[] args) {
        boolean structuredOutput = StructuredOutputStreams.configure(args);
        SpringApplication application = new SpringApplication(ProDataProcessorApplication.class);
        if (structuredOutput) {
            application.setBannerMode(Banner.Mode.OFF);
        }
        application.run(args);
    }
}

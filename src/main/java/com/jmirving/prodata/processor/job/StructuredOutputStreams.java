package com.jmirving.prodata.processor.job;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Set;

/**
 * Reserves the process's original stdout for the structured result envelope.
 */
public final class StructuredOutputStreams {
    private static final PrintStream RESULT_STREAM = System.out;
    private static final Set<String> STRUCTURED_ARGUMENTS = Set.of(
            "--structured-output",
            "--prodata.processor.structured-output",
            "--prodata.processor.structuredoutput"
    );

    private StructuredOutputStreams() {
    }

    public static boolean configure(String[] args) {
        boolean jsonRequested = isJson(System.getProperty("prodata.processor.structured-output"))
                || isJson(System.getProperty("prodata.processor.structuredOutput"))
                || isJson(System.getenv("PRODATA_PROCESSOR_STRUCTUREDOUTPUT"))
                || isJson(System.getenv("PRODATA_PROCESSOR_STRUCTURED_OUTPUT"));

        for (String argument : args) {
            int separator = argument.indexOf('=');
            if (separator > 0
                    && STRUCTURED_ARGUMENTS.contains(argument.substring(0, separator).toLowerCase(Locale.ROOT))
                    && isJson(argument.substring(separator + 1))) {
                jsonRequested = true;
                break;
            }
        }

        if (jsonRequested) {
            System.setOut(System.err);
        }
        return jsonRequested;
    }

    static PrintStream resultStream() {
        return RESULT_STREAM;
    }

    private static boolean isJson(String value) {
        return value != null && "json".equalsIgnoreCase(value.trim());
    }
}

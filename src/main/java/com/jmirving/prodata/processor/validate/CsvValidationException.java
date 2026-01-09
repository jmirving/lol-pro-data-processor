package com.jmirving.prodata.processor.validate;

public class CsvValidationException extends RuntimeException {
    public CsvValidationException(String message) {
        super(message);
    }

    public CsvValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

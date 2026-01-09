package com.jmirving.prodata.processor.validate;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvHeaderValidatorTest {

    @Test
    void validatesRequiredColumnsIgnoringCase() throws IOException {
        CsvHeaderValidator validator = new CsvHeaderValidator();
        String header = "gameid,league,split,year,date,game,patch,participantid,side,teamid," +
                "ban1,ban2,ban3,ban4,ban5,pick1,pick2,pick3,pick4,pick5";

        assertDoesNotThrow(() -> validator.validate(header));
    }

    @Test
    void throwsWhenRequiredColumnsMissing() {
        CsvHeaderValidator validator = new CsvHeaderValidator();
        String header = "gameid,league,split,year,date,game,patch,participantid,side,teamid," +
                "ban1,ban2,ban3,ban4,ban5,pick1,pick2,pick3,pick4";

        assertThrows(CsvValidationException.class, () -> validator.validate(header));
    }

    @Test
    void stripsBom() throws IOException {
        CsvHeaderValidator validator = new CsvHeaderValidator();
        String header = "\uFEFFgameid,league,split,year,date,game,patch,participantid,side,teamid," +
                "ban1,ban2,ban3,ban4,ban5,pick1,pick2,pick3,pick4,pick5";

        assertDoesNotThrow(() -> validator.validate(header));
    }
}

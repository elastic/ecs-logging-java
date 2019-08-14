package co.elastic.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TimestampSerializerTest {

    private TimestampSerializer dateSerializer;
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.of("UTC"));


    @BeforeEach
    void setUp() {
        dateSerializer = new TimestampSerializer();
    }

    @Test
    void testSerializeEpochTimestampAsIsoDateTime() {
        long timestamp = 0;
        long lastTimestampToCheck = LocalDateTime.now()
                .plus(1, ChronoUnit.YEARS)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
        // interval is approximately a hour but not exactly
        // to get different values for the minutes, seconds and milliseconds
        long interval = 997 * 61 * 61;
        for (; timestamp <= lastTimestampToCheck; timestamp += interval) {
            assertDateFormattingIsCorrect(Instant.ofEpochMilli(timestamp));
        }
        StringBuilder builder = new StringBuilder();
        dateSerializer.serializeEpochTimestampAsIsoDateTime(builder, 1565093352375L);
        System.out.println(builder);
        builder.setLength(0);
        dateSerializer.serializeEpochTimestampAsIsoDateTime(builder, 1565093352379L);
        System.out.println(builder);
        builder.setLength(0);
        dateSerializer.serializeEpochTimestampAsIsoDateTime(builder, 1565100520199L);
        System.out.println(builder);
    }



    private void assertDateFormattingIsCorrect(Instant instant) {
        StringBuilder builder = new StringBuilder();
        dateSerializer.serializeEpochTimestampAsIsoDateTime(builder, instant.toEpochMilli());
        assertThat(builder.toString()).isEqualTo(dateTimeFormatter.format(instant));
    }


}
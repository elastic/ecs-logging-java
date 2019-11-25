/*-
 * #%L
 * Java ECS logging
 * %%
 * Copyright (C) 2019 Elastic and contributors
 * %%
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * #L%
 */
package co.elastic.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class TimestampSerializerTest {

    private TimestampSerializer dateSerializer;
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.of("UTC"));

    @BeforeEach
    void setUp() {
        dateSerializer = new TimestampSerializer();
    }

    @Test
    public void testSerializeWithCustomLocale() throws InterruptedException {
        Locale.setDefault(new Locale.Builder()
                .setLanguage("uz")
                .setRegion("UZ")
                .setScript("Cyrl")
                .build());

        dateSerializer = new TimestampSerializer();

        long timestamp = Instant.now().toEpochMilli();
        assertDateFormattingIsCorrect(Instant.ofEpochMilli(timestamp));
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

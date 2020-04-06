/*-
 * #%L
 * Java ECS logging
 * %%
 * Copyright (C) 2019 - 2020 Elastic and contributors
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
package co.elastic.logging.jul;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EcsFormatterTest {

    private final EcsFormatter formatter = new EcsFormatter();

    private final LogRecord record = new LogRecord(Level.INFO, "Example Meesage");

    @Test
    public void testFormatWithIncludeOriginFlag() {

        formatter.setIncludeOrigin(true);

        final String result = formatter.format(record);

        assertThat(result).isEqualTo(
                "{\"@timestamp\":\"1970-01-01T00:00:00.005Z\", \"log.level\": \"INFO\", \"message\":\"Example Meesage\", \"process.thread.id\":7,\"log.logger\":\"ExampleLogger\",\"log.origin\":{\"file.name\":\"ExampleClass.java\",\"function\":\"exampleMethod\"}}\n");
    }

    @Test
    public void testFormatWithoutIncludeOriginFlag() {

        final String result = formatter.format(record);

        assertThat(result).isEqualTo(
                "{\"@timestamp\":\"1970-01-01T00:00:00.005Z\", \"log.level\": \"INFO\", \"message\":\"Example Meesage\", \"process.thread.id\":7,\"log.logger\":\"ExampleLogger\"}\n");
    }

    @Test
    public void testFormatWithoutLoggerName() {
        record.setLoggerName(null);

        final String result = formatter.format(record);

        assertThat(result).isEqualTo(
                "{\"@timestamp\":\"1970-01-01T00:00:00.005Z\", \"log.level\": \"INFO\", \"message\":\"Example Meesage\", \"process.thread.id\":7}\n");
    }

    @Test
    public void testFormatWithEmptyLoggerName() {
        record.setLoggerName("");

        final String result = formatter.format(record);

        assertThat(result).isEqualTo(
                "{\"@timestamp\":\"1970-01-01T00:00:00.005Z\", \"log.level\": \"INFO\", \"message\":\"Example Meesage\", \"process.thread.id\":7,\"log.logger\":\"\"}\n");
    }

    @Test
    public void testFormatWithInnerClassName() {
        formatter.setIncludeOrigin(true);
        record.setSourceClassName("test.ExampleClass$InnerClass");

        final String result = formatter.format(record);

        assertThat(result).isEqualTo(
                "{\"@timestamp\":\"1970-01-01T00:00:00.005Z\", \"log.level\": \"INFO\", \"message\":\"Example Meesage\", \"process.thread.id\":7,\"log.logger\":\"ExampleLogger\",\"log.origin\":{\"file.name\":\"ExampleClass.java\",\"function\":\"exampleMethod\"}}\n");
    }

    @Test
    public void testFormatWithInvalidClassName() {
        formatter.setIncludeOrigin(true);
        record.setSourceClassName("$test.ExampleClass");

        final String result = formatter.format(record);

        assertThat(result).isEqualTo(
                "{\"@timestamp\":\"1970-01-01T00:00:00.005Z\", \"log.level\": \"INFO\", \"message\":\"Example Meesage\", \"process.thread.id\":7,\"log.logger\":\"ExampleLogger\",\"log.origin\":{\"file.name\":\"<Unknown>\",\"function\":\"exampleMethod\"}}\n");
    }

    @BeforeEach
    void setUp() {
        record.setInstant(Instant.ofEpochMilli(5));
        record.setSourceClassName("ExampleClass");
        record.setSourceMethodName("exampleMethod");
        record.setThreadID(7);
        record.setLoggerName("ExampleLogger");
    }

}

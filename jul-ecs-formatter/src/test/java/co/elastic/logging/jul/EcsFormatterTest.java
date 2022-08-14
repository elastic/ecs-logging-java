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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.assertj.core.api.Assertions.assertThat;

public class EcsFormatterTest {

    private final EcsFormatter formatter = new EcsFormatter();

    private final LogRecord record = new LogRecord(Level.INFO, "Example Message");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        record.setInstant(Instant.ofEpochMilli(5));
        record.setSourceClassName("ExampleClass");
        record.setSourceMethodName("exampleMethod");
        record.setThreadID(7);
        record.setLoggerName("ExampleLogger");
    }

    @Test
    public void testFormatWithIncludeOriginFlag() throws Exception {
        formatter.setIncludeOrigin(true);

        final String result = formatter.format(record);

        assertThat(parseJson(result).at("/log/origin/file/name").textValue()).isEqualTo("ExampleClass.java");
        assertThat(parseJson(result).at("/log/origin/function").textValue()).isEqualTo("exampleMethod");
    }

    @Test
    public void testFormatWithoutIncludeOriginFlag() throws Exception {
        final JsonNode result = parseJson(formatter.format(record));
        assertThat(result.get("log.origin")).isNull();
    }

    @Test
    public void testFormatWithoutLoggerName() throws Exception {
        record.setLoggerName(null);

        final JsonNode result = parseJson(formatter.format(record));

        assertThat(result.get("log.logger")).isNull();
    }

    @Test
    public void testFormatWithEmptyLoggerName() throws Exception {
        record.setLoggerName("");

        final JsonNode result = parseJson(formatter.format(record));

        assertThat(result.get("log.logger").textValue()).isEmpty();
    }

    @Test
    public void testFormatWithInnerClassName() throws Exception {
        formatter.setIncludeOrigin(true);
        record.setSourceClassName("test.ExampleClass$InnerClass");

        JsonNode result = parseJson(formatter.format(record));
        assertThat(result.at("/log/origin/file/name").textValue()).isEqualTo("ExampleClass.java");
        assertThat(result.at("/log/origin/function").textValue()).isEqualTo("exampleMethod");
    }

    @Test
    public void testFormatWithInvalidClassName() throws Exception {
        formatter.setIncludeOrigin(true);
        record.setSourceClassName("$test.ExampleClass");

        JsonNode result = parseJson(formatter.format(record));
        assertThat(result.at("/log/origin/file/name").textValue()).isEqualTo("<Unknown>");
        assertThat(result.at("/log/origin/function").textValue()).isEqualTo("exampleMethod");
    }

    @Test
    void testMdcSerialization_singleEntry() {
        Map<String,String> mdc = new HashMap<>();
        TestMdcEcsFormatter mdcFormatter = new TestMdcEcsFormatter(mdc);
        mdc.put("mdc.key", "value");
        JsonNode result = parseJson(mdcFormatter.format(record));
        assertThat(result.get("mdc.key").textValue()).isEqualTo("value");
    }

    private static JsonNode parseJson(String formatter) {
        try {
            return objectMapper.readTree(formatter);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static class TestMdcEcsFormatter extends EcsFormatter {
        private final Map<String, String> mdc;

        public TestMdcEcsFormatter(Map<String, String> mdc) {
            this.mdc = mdc;
        }

        @Override
        protected Map<String, String> getMdcEntries() {
            return mdc;
        }
    }

}

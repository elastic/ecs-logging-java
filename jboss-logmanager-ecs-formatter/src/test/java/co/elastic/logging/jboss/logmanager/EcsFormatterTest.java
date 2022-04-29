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
package co.elastic.logging.jboss.logmanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EcsFormatterTest {

    private final EcsFormatter formatter = new EcsFormatter();
    private final ExtLogRecord record = new ExtLogRecord(Level.INFO, "Example Message", "ExampleLoggerClass");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        record.setInstant(Instant.ofEpochMilli(5));
        record.setLoggerName("ExampleLogger");
        record.setSourceFileName("ExampleSourceFile.java");
        record.setSourceClassName("ExampleSourceClass");
        record.setSourceMethodName("exampleSourceMethod");
        record.setSourceLineNumber(10);
        record.setThreadName("ExampleThread");
    }

    @Test
    void testSingleNDCInformationLogging() throws Exception {
        record.setNdc("exampleTag");
        JsonNode result = objectMapper.readTree(formatter.format(record));
        assertThat(result.get("tags")).hasSize(1);
        assertThat(result.get("tags").get(0).textValue()).isEqualTo("exampleTag");
    }

    @Test
    void testMultipleNDCInformationLogging() throws Exception {
        record.setNdc("exampleTag1.exampleTag2");

        JsonNode result = objectMapper.readTree(formatter.format(record));
        assertThat(result.get("tags")).hasSize(2);
        assertThat(result.get("tags").get(0).textValue()).isEqualTo("exampleTag1");
        assertThat(result.get("tags").get(1).textValue()).isEqualTo("exampleTag2");
    }

    @Test
    void testExceptionLogging() throws Exception {
        record.setThrown(new RuntimeException("Example Exception Message") {
            @Override
            public void printStackTrace(PrintWriter pw) {
                pw.println("co.elastic.logging.jboss.logmanager.EcsFormatterTest$1: Example Exception Message");
                pw.println("\tat co.elastic.logging.jboss.logmanager.EcsFormatterTest.testExceptionLogging(EcsFormatterTest.java:125)");
            }
        });

        JsonNode result = objectMapper.readTree(formatter.format(record));
        assertThat(result.get("error.type").textValue()).isEqualTo("co.elastic.logging.jboss.logmanager.EcsFormatterTest$1");
        assertThat(result.get("error.message").textValue()).isEqualTo("Example Exception Message");
        assertThat(result.get("error.stack_trace").textValue())
                .isEqualTo("co.elastic.logging.jboss.logmanager.EcsFormatterTest$1: Example Exception Message" + System.lineSeparator() +
                        "\tat co.elastic.logging.jboss.logmanager.EcsFormatterTest.testExceptionLogging(EcsFormatterTest.java:125)" + System.lineSeparator());
    }
}


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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.time.Instant;
import java.util.Map;

import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EcsFormatterTest {

    private final EcsFormatter formatter = new EcsFormatter();

    private final ExtLogRecord record = new ExtLogRecord(Level.INFO, "Example Message", "ExampleLoggerClass");

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
    void testSingleNDCInformationLogging() {
        record.setNdc("exampleTag");

        assertThat(formatter.format(record)).isEqualTo("{" +
                "\"@timestamp\":\"1970-01-01T00:00:00.005Z\", " +
                "\"log.level\": \"INFO\", " +
                "\"message\":\"Example Message\", " +
                "\"process.thread.name\":\"ExampleThread\"," +
                "\"log.logger\":\"ExampleLogger\"," +
                "\"tags\":[\"exampleTag\"]" +
                "}\n");
    }

    @Test
    void testMultipleNDCInformationLogging() {
        record.setNdc("exampleTag1.exampleTag2");

        assertThat(formatter.format(record)).isEqualTo("{" +
                "\"@timestamp\":\"1970-01-01T00:00:00.005Z\", " +
                "\"log.level\": \"INFO\", " +
                "\"message\":\"Example Message\", " +
                "\"process.thread.name\":\"ExampleThread\"," +
                "\"log.logger\":\"ExampleLogger\"," +
                "\"tags\":[\"exampleTag1\",\"exampleTag2\"]" +
                "}\n");
    }

    @Test
    void testExceptionLogging() {
        record.setThrown(new RuntimeException("Example Exception Message") {
            @Override
            public void printStackTrace(PrintWriter pw) {
                pw.println("co.elastic.logging.jboss.logmanager.EcsFormatterTest$1: Example Exception Message");
                pw.println("\tat co.elastic.logging.jboss.logmanager.EcsFormatterTest.testExceptionLogging(EcsFormatterTest.java:125)");
            }
        });

        assertThat(formatter.format(record).replace("\\r\\n", "\\n")).isEqualTo("{" +
                "\"@timestamp\":\"1970-01-01T00:00:00.005Z\", " +
                "\"log.level\": \"INFO\", " +
                "\"message\":\"Example Message\", " +
                "\"process.thread.name\":\"ExampleThread\"," +
                "\"log.logger\":\"ExampleLogger\"," +
                "\"error.type\":\"co.elastic.logging.jboss.logmanager.EcsFormatterTest$1\"," +
                "\"error.message\":\"Example Exception Message\"," +
                "\"error.stack_trace\":\"co.elastic.logging.jboss.logmanager.EcsFormatterTest$1: Example Exception Message\\n\\tat co.elastic.logging.jboss.logmanager.EcsFormatterTest.testExceptionLogging(EcsFormatterTest.java:125)\\n\"" +
                "}\n");
    }
}


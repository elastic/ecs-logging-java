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
package co.elastic.logging;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.within;

public abstract class AbstractEcsLoggingTest {

    protected ObjectMapper objectMapper = new ObjectMapper();
    private JsonNode spec;

    @BeforeEach
    final void setUpSpec() throws Exception {
        spec = objectMapper.readTree(getClass().getClassLoader().getResource("spec/spec.json"));
    }

    @Test
    void testMetadata() throws Exception {
        debug("test");
        assertThat(getAndValidateLastLogLine().get("process.thread.name").textValue()).isEqualTo(Thread.currentThread().getName());
        assertThat(getAndValidateLastLogLine().get("service.name").textValue()).isEqualTo("test");
        assertThat(Instant.parse(getAndValidateLastLogLine().get("@timestamp").textValue())).isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
        assertThat(getAndValidateLastLogLine().get("log.level").textValue()).isIn("DEBUG", "FINE");
        assertThat(getAndValidateLastLogLine().get("log.logger")).isNotNull();
        assertThat(getAndValidateLastLogLine().get("event.dataset").textValue()).isEqualTo("testdataset.log");
        assertThat(getAndValidateLastLogLine().get("ecs.version").textValue()).isEqualTo("1.2.0");
        validateLog(getAndValidateLastLogLine());
    }

    @Test
    void testSimpleLog() throws Exception {
        debug("test");
        assertThat(getAndValidateLastLogLine().get("message").textValue()).isEqualTo("test");
    }

    void validateLog(JsonNode logLine) {
        Iterator<Map.Entry<String, JsonNode>> fields = spec.get("fields").fields();
        Iterator<String> iterator = logLine.fieldNames();
        List<String> logFieldNames = new ArrayList<>();
        iterator.forEachRemaining(logFieldNames::add);

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (field.getValue().get("required").booleanValue()) {
                assertThat(logLine.get(field.getKey())).isNotNull();
            }
            JsonNode fieldInLog = logLine.get(field.getKey());
            if (fieldInLog != null) {
                JsonNode index = field.getValue().get("index");
                if (index != null) {
                    assertThat(logFieldNames.get(index.intValue()))
                            .describedAs(logLine.toString())
                            .isEqualTo(field.getKey());
                }
                String type = field.getValue().get("type").textValue();
                switch (type) {
                    case "datetime":
                        assertThatCode(() -> Instant.parse(fieldInLog.textValue())).doesNotThrowAnyException();
                        break;
                    case "string":
                        assertThat(fieldInLog.isTextual()).isTrue();
                }
            }
        }
    }

    @Test
    void testSimpleParameterizedLog() throws Exception {
        ParameterizedLogSupport parameterizedLogSupport = getParameterizedLogSettings();

        // don't test parameterized logging if the log framework implementation does not support it.
        if (parameterizedLogSupport == ParameterizedLogSupport.NOT_SUPPORTED) {
            return;
        }

        if (parameterizedLogSupport == ParameterizedLogSupport.NUMBER_AND_BRACKETS) {
            debug("{0} is not {1}", 1, 2);
        } else if (parameterizedLogSupport == ParameterizedLogSupport.BRACKETS_ONLY) {
            debug("{} is not {}", 1, 2);
        }

        assertThat(getAndValidateLastLogLine().get("message").textValue()).isEqualTo("1 is not 2");
    }

    @Test
    void testThreadContext() throws Exception {
        if (putMdc("foo", "bar")) {
            debug("test");
            assertThat(getAndValidateLastLogLine().get("foo").textValue()).isEqualTo("bar");
        }
    }

    @Test
    void testThreadContextStack() throws Exception {
        if (putNdc("foo")) {
            debug("test");
            assertThat(getAndValidateLastLogLine().get("tags").iterator().next().textValue()).isEqualTo("foo");
        }
    }

    @Test
    void testMdc() throws Exception {
        if (putMdc("transaction.id", "0af7651916cd43dd8448eb211c80319c")) {
            putMdc("span.id", "foo");
            putMdc("foo", "bar");
            debug("test");
            assertThat(getAndValidateLastLogLine().get("labels.transaction.id")).isNull();
            assertThat(getAndValidateLastLogLine().get("transaction.id").textValue())
                    .isEqualTo("0af7651916cd43dd8448eb211c80319c");
            assertThat(getAndValidateLastLogLine().get("span.id").textValue()).isEqualTo("foo");
            assertThat(getAndValidateLastLogLine().get("foo").textValue()).isEqualTo("bar");
        }
    }

    @Test
    void testLogException() throws Exception {
        error("test", new RuntimeException("test"));
        assertThat(getAndValidateLastLogLine().get("log.level").textValue()).isEqualTo("ERROR");
        assertThat(getAndValidateLastLogLine().get("error.message").textValue()).isEqualTo("test");
        assertThat(getAndValidateLastLogLine().get("error.type").textValue()).isEqualTo(RuntimeException.class.getName());
        String stackTrace = StreamSupport.stream(getAndValidateLastLogLine().get("error.stack_trace").spliterator(), false)
                .map(JsonNode::textValue)
                .collect(Collectors.joining("\n", "", "\n"));
        assertThat(stackTrace).contains("at co.elastic.logging.AbstractEcsLoggingTest.testLogException");
    }

    @Test
    void testLogExceptionNullMessage() throws Exception {
        error("test", new RuntimeException());
        assertThat(getAndValidateLastLogLine().get("error.message")).isNull();
        assertThat(getAndValidateLastLogLine().get("error.type").textValue()).isEqualTo(RuntimeException.class.getName());
    }

    @Test
    void testLogOrigin() throws Exception {
        debug("test");
        assertThat(getAndValidateLastLogLine().get("log.origin").get("file.name").textValue()).endsWith(".java");
        assertThat(getAndValidateLastLogLine().get("log.origin").get("function").textValue()).isEqualTo("debug");
        assertThat(getAndValidateLastLogLine().get("log.origin").get("file.line").intValue()).isPositive();
    }

    public boolean putMdc(String key, String value) {
        return false;
    }

    public boolean putNdc(String message) {
        return false;
    }

    public ParameterizedLogSupport getParameterizedLogSettings() {
        return ParameterizedLogSupport.BRACKETS_ONLY;
    }

    public abstract void debug(String message);

    public abstract void debug(String message, Object... logParams);

    public abstract void error(String message, Throwable t);

    public final JsonNode getAndValidateLastLogLine() throws IOException {
        JsonNode log = getLastLogLine();
        validateLog(log);
        return log;
    }

    public abstract JsonNode getLastLogLine() throws IOException;
}

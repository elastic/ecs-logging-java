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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.within;

public abstract class AbstractEcsLoggingTest {

    protected ObjectMapper objectMapper = new ObjectMapper();
    protected JsonNode spec;

    @BeforeEach
    final void setUpSpec() throws Exception {
        spec = objectMapper.readTree(getClass().getClassLoader().getResource("spec/spec.json"));
    }

    @Test
    void testMetadata() throws Exception {
        debug("test");
        JsonNode logLine = getAndValidateLastLogLine();
        System.out.println(logLine.toPrettyString());
        assertThat(logLine.get("process.thread.name").textValue()).isEqualTo(Thread.currentThread().getName());
        assertThat(logLine.get("service.name").textValue()).isEqualTo("test");
        assertThat(logLine.get("service.node.name").textValue()).isEqualTo("test-node");
        assertThat(Instant.parse(logLine.get("@timestamp").textValue())).isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
        assertThat(logLine.get("log.level").textValue()).isIn("DEBUG", "FINE");
        assertThat(logLine.get("log.logger")).isNotNull();
        assertThat(logLine.get("event.dataset").textValue()).isEqualTo("testdataset");
        assertThat(logLine.get("ecs.version").textValue()).isEqualTo("1.2.0");
        validateLog(logLine);
    }

    @Test
    protected final void testAdditionalFields() throws Exception {
        debug("test");
        JsonNode logLine = getAndValidateLastLogLine();
        System.out.println(logLine.toPrettyString());
        assertThat(logLine.get("key1").textValue()).isEqualTo("value1");
        assertThat(logLine.get("key2").textValue()).isEqualTo("value2");
        validateLog(logLine);
    }

    @Test
    void testSimpleLog() throws Exception {
        debug("test");
        assertThat(getAndValidateLastLogLine().get("message").textValue()).isEqualTo("test");
    }

    void validateLog(JsonNode logLine) {
        Iterator<Map.Entry<String, JsonNode>> specFields = spec.get("fields").fields();
        Iterator<String> iterator = logLine.fieldNames();
        List<String> logFieldNames = new ArrayList<>();
        iterator.forEachRemaining(logFieldNames::add);

        while (specFields.hasNext()) {
            Map.Entry<String, JsonNode> specField = specFields.next();
            String specFieldName = specField.getKey();
            JsonNode specForField = specField.getValue();
            JsonNode fieldInLog = logLine.get(specFieldName);
            if (fieldInLog == null) {
                fieldInLog = logLine.at("/" + specFieldName.replace('.', '/'));
            }

            validateRequiredField(logLine, specFieldName, fieldInLog, specForField.get("required").booleanValue());
            if (!fieldInLog.isMissingNode()) {
                validateIndex(logLine, logFieldNames, specFieldName, specForField.get("index"));
                validateType(fieldInLog, specForField.get("type").textValue());
                validateNesting(logLine, specFieldName, specForField.has("top_level_field") && specForField.get("top_level_field").asBoolean(false));
            }
        }
    }

    private void validateNesting(JsonNode logLine, String specFieldName, boolean topLevelField) {
        if (topLevelField) {
            assertThat(logLine.at("/" + specFieldName.replace('.', '/')).isMissingNode()).isTrue();
        }
    }

    private void validateRequiredField(JsonNode logLine, String specFieldName, JsonNode fieldInLog, boolean required) {
        if (required) {
            assertThat(fieldInLog.isMissingNode())
                    .describedAs("Expected %s to be non-null: %s", specFieldName, logLine)
                    .isFalse();
        }
    }

    private void validateIndex(JsonNode logLine, List<String> logFieldNames, String specFieldName, JsonNode index) {
        if (index != null) {
            assertThat(logFieldNames.get(index.intValue()))
                    .describedAs(logLine.toString())
                    .isEqualTo(specFieldName);
        }
    }

    private void validateType(JsonNode fieldInLog, String type) {
        switch (type) {
            case "datetime":
                assertThatCode(() -> Instant.parse(fieldInLog.textValue())).doesNotThrowAnyException();
            case "string":
                assertThat(fieldInLog.isTextual())
                        .describedAs("%s is not a %s", fieldInLog, type)
                        .isTrue();
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
        JsonNode log = getAndValidateLastLogLine();
        System.out.println(log.toPrettyString());
        assertThat(log.get("log.level").textValue()).isIn("ERROR", "SEVERE");
        assertThat(log.get("error.message").textValue()).isEqualTo("test");
        assertThat(log.get("error.type").textValue()).isEqualTo(RuntimeException.class.getName());
        assertThat(log.get("error.stack_trace").textValue()).contains("at co.elastic.logging.AbstractEcsLoggingTest.testLogException");
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
        JsonNode logLine = getAndValidateLastLogLine();
        System.out.println(logLine.toPrettyString());
        assertThat(logLine.at("/log/origin/file/name").textValue()).endsWith(".java");
        assertThat(logLine.at("/log/origin/function").textValue()).isEqualTo("debug");
        assertThat(logLine.at("/log/origin/file/line").intValue()).isPositive();
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

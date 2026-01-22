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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class EcsJsonSerializerTest {

    private static final String ERROR_TYPE = "error.type";
    private static final String ERROR_STACK_TRACE = "error.stack_trace";
    private static final String ERROR_MESSAGE = "error.message";
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializeExceptionAsString() throws IOException {
        Exception exception = new Exception("foo");
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append('{');
        EcsJsonSerializer.serializeException(jsonBuilder, exception, false);
        jsonBuilder.append('}');
        JsonNode jsonNode = objectMapper.readTree(jsonBuilder.toString());

        assertThat(jsonNode.get(ERROR_TYPE).textValue()).isEqualTo(exception.getClass().getName());
        assertThat(jsonNode.get(ERROR_MESSAGE).textValue()).isEqualTo("foo");
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        assertThat(jsonNode.get(ERROR_STACK_TRACE).textValue()).isEqualTo(stringWriter.toString());
    }

    @Test
    void serializeExceptionWithoutStacktraceAsString() throws IOException {
        Exception exception = new Exception("no stacktrace"){
            @Override
            public synchronized Throwable fillInStackTrace() {
                return this;
            }
        };
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append('{');
        EcsJsonSerializer.serializeException(jsonBuilder, exception, false);
        jsonBuilder.append('}');
        JsonNode jsonNode = objectMapper.readTree(jsonBuilder.toString());

        assertThat(jsonNode.get(ERROR_TYPE).textValue()).isEqualTo(exception.getClass().getName());
        assertThat(jsonNode.get(ERROR_MESSAGE).textValue()).isEqualTo("no stacktrace");
        assertThat(jsonNode.get(ERROR_STACK_TRACE)).isNull();
    }

    @Test
    void testEscaping() throws IOException {
        String loggerName = "logger\"";
        String serviceName = "test\"";
        String serviceVersion = "test-version\"";
        String serviceEnvironment = "test-environment\"";
        String serviceNodeName = "test-node\"";
        String eventDataset = "event-dataset\"";
        String threadName = "event-dataset\"";
        String additionalKey = "key\"";
        String additionalValue = "=value\"";

        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append('{');
        EcsJsonSerializer.serializeLoggerName(jsonBuilder, loggerName);
        EcsJsonSerializer.serializeServiceName(jsonBuilder, serviceName);
        EcsJsonSerializer.serializeServiceVersion(jsonBuilder, serviceVersion);
        EcsJsonSerializer.serializeServiceEnvironment(jsonBuilder, serviceEnvironment);
        EcsJsonSerializer.serializeServiceNodeName(jsonBuilder, serviceNodeName);
        EcsJsonSerializer.serializeEventDataset(jsonBuilder, eventDataset);
        EcsJsonSerializer.serializeThreadName(jsonBuilder, threadName);
        EcsJsonSerializer.serializeAdditionalFields(jsonBuilder, List.of(new AdditionalField(additionalKey, additionalValue)));
        EcsJsonSerializer.serializeObjectEnd(jsonBuilder);
        jsonBuilder.append('}');
        JsonNode jsonNode = new ObjectMapper().readTree(jsonBuilder.toString());

        assertThat(jsonNode.get("log.logger").textValue()).isEqualTo(loggerName);
        assertThat(jsonNode.get("service.name").textValue()).isEqualTo(serviceName);
        assertThat(jsonNode.get("service.version").textValue()).isEqualTo(serviceVersion);
        assertThat(jsonNode.get("service.environment").textValue()).isEqualTo(serviceEnvironment);
        assertThat(jsonNode.get("service.node.name").textValue()).isEqualTo(serviceNodeName);
        assertThat(jsonNode.get("event.dataset").textValue()).isEqualTo(eventDataset);
        assertThat(jsonNode.get("process.thread.name").textValue()).isEqualTo(eventDataset);
        assertThat(jsonNode.get("process.thread.name").textValue()).isEqualTo(eventDataset);
        assertThat(jsonNode.get(additionalKey).textValue()).isEqualTo(additionalValue);
    }

    @Test
    void serializeNullDoesNotThrowAnException() throws JsonProcessingException {
        StringBuilder stringBuilder = new StringBuilder();
        EcsJsonSerializer.serializeFormattedMessage(stringBuilder, null);
        assertThat(stringBuilder.toString()).isEqualTo("\"message\":\"null\",");
    }

    @Test
    void serializeExceptionAsArray() throws IOException {
        Exception exception = new Exception("foo");
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append('{');
        EcsJsonSerializer.serializeException(jsonBuilder, exception, true);
        jsonBuilder.append('}');
        System.out.println(jsonBuilder);
        JsonNode jsonNode = objectMapper.readTree(jsonBuilder.toString());

        assertThat(jsonNode.get(ERROR_TYPE).textValue()).isEqualTo(exception.getClass().getName());
        assertThat(jsonNode.get(ERROR_MESSAGE).textValue()).isEqualTo("foo");
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        assertThat(StreamSupport.stream(jsonNode.get(ERROR_STACK_TRACE).spliterator(), false)
                .map(JsonNode::textValue)
                .collect(Collectors.joining(System.lineSeparator(), "", System.lineSeparator())))
                .isEqualTo(stringWriter.toString());
    }

    @Test
    void serializeExceptionWithoutStacktraceAsArray() throws IOException {
        Exception exception = new Exception("no stacktrace"){
            @Override
            public synchronized Throwable fillInStackTrace() {
                return this;
            }
        };
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append('{');
        EcsJsonSerializer.serializeException(jsonBuilder, exception, true);
        jsonBuilder.append('}');
        System.out.println(jsonBuilder);
        JsonNode jsonNode = objectMapper.readTree(jsonBuilder.toString());

        assertThat(jsonNode.get(ERROR_TYPE).textValue()).isEqualTo(exception.getClass().getName());
        assertThat(jsonNode.get(ERROR_MESSAGE).textValue()).isEqualTo("no stacktrace");
        assertThat(jsonNode.get(ERROR_STACK_TRACE)).isNull();
    }

    @Test
    void testRemoveIfEndsWith() {
        assertRemoveIfEndsWith("", "foo", "");
        assertRemoveIfEndsWith("foobar", "foo", "foobar");
        assertRemoveIfEndsWith("barfoo", "foo", "bar");
    }

    @Test
    void serializeException() throws JsonProcessingException {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append('{');
        EcsJsonSerializer.serializeException(jsonBuilder, "className", "message", "stacktrace\ncaused by error", false);
        jsonBuilder.append('}');

        JsonNode jsonNode = objectMapper.readTree(jsonBuilder.toString());
        assertThat(jsonNode.get(ERROR_TYPE).textValue()).isEqualTo("className");
        assertThat(jsonNode.get(ERROR_STACK_TRACE).isArray()).isFalse();
        assertThat(jsonNode.get(ERROR_STACK_TRACE).textValue()).isEqualTo("stacktrace\ncaused by error");
        assertThat(jsonNode.get(ERROR_MESSAGE).textValue()).isEqualTo("message");
    }

    @Test
    void serializeExceptionWithStackTraceAsArray() throws JsonProcessingException {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append('{');
        EcsJsonSerializer.serializeException(jsonBuilder, "className", "message", "stacktrace\ncaused by error", true);
        jsonBuilder.append('}');

        JsonNode jsonNode = objectMapper.readTree(jsonBuilder.toString());
        System.out.println(jsonNode.toPrettyString());
        assertThat(jsonNode.get(ERROR_TYPE).textValue()).isEqualTo("className");
        assertThat(jsonNode.get(ERROR_STACK_TRACE).isArray()).isTrue();
        assertThat(jsonNode.get(ERROR_STACK_TRACE).size()).isEqualTo(2);
        assertThat(jsonNode.get(ERROR_STACK_TRACE).get(0).textValue()).isEqualTo("stacktrace");
        assertThat(jsonNode.get(ERROR_STACK_TRACE).get(1).textValue()).isEqualTo("caused by error");
        assertThat(jsonNode.get(ERROR_MESSAGE).textValue()).isEqualTo("message");
    }

    @Test
    void serializeExceptionWithSingleLineStackTraceAsArray() throws JsonProcessingException {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append('{');
        EcsJsonSerializer.serializeException(jsonBuilder, "className", "message", "caused by error", true);
        jsonBuilder.append('}');
        System.out.println(jsonBuilder);
        JsonNode jsonNode = objectMapper.readTree(jsonBuilder.toString());
        System.out.println(jsonNode.toPrettyString());
        assertThat(jsonNode.get(ERROR_TYPE).textValue()).isEqualTo("className");
        assertThat(jsonNode.get(ERROR_STACK_TRACE).isArray()).isTrue();
        assertThat(jsonNode.get(ERROR_STACK_TRACE).size()).isEqualTo(1);
        assertThat(jsonNode.get(ERROR_STACK_TRACE).get(0).textValue()).isEqualTo("caused by error");
        assertThat(jsonNode.get(ERROR_MESSAGE).textValue()).isEqualTo("message");
    }

    @Test
    void serializeExceptionWithNullMessage() throws JsonProcessingException {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append('{');
        EcsJsonSerializer.serializeException(jsonBuilder, "className", null, "stacktrace", false);
        jsonBuilder.append('}');

        JsonNode jsonNode = objectMapper.readTree(jsonBuilder.toString());
        assertThat(jsonNode.get(ERROR_TYPE).textValue()).isEqualTo("className");
        assertThat(jsonNode.get(ERROR_STACK_TRACE).textValue()).isEqualTo("stacktrace");
        assertThat(jsonNode.get(ERROR_MESSAGE)).isNull();
    }

    private void assertRemoveIfEndsWith(String builder, String ending, String expected) {
        StringBuilder sb = new StringBuilder(builder);
        EcsJsonSerializer.removeIfEndsWith(sb, ending);
        assertThat(sb.toString()).isEqualTo(expected);
    }
}

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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public abstract class AbstractEcsLoggingTest {

    protected ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testMetadata() throws Exception {
        debug("test");
        assertThat(getLastLogLine().get("process.thread.name").textValue()).isEqualTo(Thread.currentThread().getName());
        assertThat(getLastLogLine().get("service.name").textValue()).isEqualTo("test");
        assertThat(Instant.parse(getLastLogLine().get("@timestamp").textValue())).isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
        assertThat(getLastLogLine().get("log.level").textValue()).isEqualTo("DEBUG");
        assertThat(getLastLogLine().get("log.logger")).isNotNull();
        assertThat(getLastLogLine().get("event.dataset").textValue()).isEqualTo("testdataset.log");
    }

    @Test
    void testSimpleLog() throws Exception {
        debug("test");
        assertThat(getLastLogLine().get("message").textValue()).isEqualTo("test");
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

        assertThat(getLastLogLine().get("message").textValue()).isEqualTo("1 is not 2");
    }

    @Test
    void testThreadContext() throws Exception {
        if (putMdc("foo", "bar")) {
            debug("test");
            assertThat(getLastLogLine().get("foo").textValue()).isEqualTo("bar");
        }
    }

    @Test
    void testThreadContextStack() throws Exception {
        if (putNdc("foo")) {
            debug("test");
            assertThat(getLastLogLine().get("tags").iterator().next().textValue()).isEqualTo("foo");
        }
    }

    @Test
    void testMdc() throws Exception {
        if (putMdc("transaction.id", "0af7651916cd43dd8448eb211c80319c")) {
            putMdc("span.id", "foo");
            putMdc("foo", "bar");
            debug("test");
            assertThat(getLastLogLine().get("labels.transaction.id")).isNull();
            assertThat(getLastLogLine().get("transaction.id").textValue())
                    .isEqualTo("0af7651916cd43dd8448eb211c80319c");
            assertThat(getLastLogLine().get("span.id").textValue()).isEqualTo("foo");
            assertThat(getLastLogLine().get("foo").textValue()).isEqualTo("bar");
        }
    }

    @Test
    void testLogException() throws Exception {
        error("test", new RuntimeException("test"));
        assertThat(getLastLogLine().get("log.level").textValue()).isEqualTo("ERROR");
        assertThat(getLastLogLine().get("error.message").textValue()).isEqualTo("test");
        assertThat(getLastLogLine().get("error.type").textValue()).isEqualTo(RuntimeException.class.getName());
        String stackTrace = StreamSupport.stream(getLastLogLine().get("error.stack_trace").spliterator(), false)
                .map(JsonNode::textValue)
                .collect(Collectors.joining("\n", "", "\n"));
        assertThat(stackTrace).contains("at co.elastic.logging.AbstractEcsLoggingTest.testLogException");
    }

    @Test
    void testLogExceptionNullMessage() throws Exception {
        error("test", new RuntimeException());
        assertThat(getLastLogLine().get("error.message")).isNull();
        assertThat(getLastLogLine().get("error.type").textValue()).isEqualTo(RuntimeException.class.getName());
    }

    @Test
    void testLogOrigin() throws Exception {
        debug("test");
        assertThat(getLastLogLine().get("log.origin").get("file.name").textValue()).endsWith(".java");
        assertThat(getLastLogLine().get("log.origin").get("function").textValue()).isEqualTo("debug");
        assertThat(getLastLogLine().get("log.origin").get("file.line").intValue()).isPositive();
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

    public abstract JsonNode getLastLogLine() throws IOException;
}

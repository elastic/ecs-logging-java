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
    }

    @Test
    void testSimpleLog() throws Exception {
        debug("test");
        assertThat(getLastLogLine().get("message").textValue()).isEqualTo("test");
    }

    @Test
    void testThreadContext() throws Exception {
        putMdc("foo", "bar");
        debug("test");
        assertThat(getLastLogLine().get("labels.foo").textValue()).isEqualTo("bar");
    }

    @Test
    void testThreadContextStack() throws Exception {
        if (putNdc("foo")) {
            debug("test");
            assertThat(getLastLogLine().get("tags").iterator().next().textValue()).isEqualTo("foo");
        }
    }

    @Test
    void testTopLevelLabels() throws Exception {
        putMdc("transaction.id", "0af7651916cd43dd8448eb211c80319c");
        debug("test");
        assertThat(getLastLogLine().get("labels.transaction.id")).isNull();
        assertThat(getLastLogLine().get("transaction.id").textValue()).isEqualTo("0af7651916cd43dd8448eb211c80319c");
    }

    @Test
    void testLogException() throws Exception {
        error("test", new RuntimeException("test"));
        assertThat(getLastLogLine().get("log.level").textValue()).isEqualTo("ERROR");
        assertThat(getLastLogLine().get("error.message").textValue()).isEqualTo("test");
        assertThat(getLastLogLine().get("error.code").textValue()).isEqualTo(RuntimeException.class.getName());
        String stackTrace = StreamSupport.stream(getLastLogLine().get("error.stack_trace").spliterator(), false)
                .map(JsonNode::textValue)
                .collect(Collectors.joining("\n", "", "\n"));
        assertThat(stackTrace).contains("at co.elastic.logging.AbstractEcsLoggingTest.testLogException");
    }

    public abstract void putMdc(String key, String value);

    public boolean putNdc(String message) {
        return false;
    }

    public abstract void debug(String message);

    public abstract void error(String message, Throwable t);

    public abstract JsonNode getLastLogLine() throws IOException;
}

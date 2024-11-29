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
package co.elastic.logging.log4j2;

import co.elastic.logging.AbstractEcsLoggingTest;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.elastic.logging.log4j2.CustomMdcSerializer.CUSTOM_MDC_SERIALIZER_TEST_KEY;
import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractLog4j2EcsLayoutTest extends AbstractEcsLoggingTest {
    protected Logger root;
    protected ListAppender listAppender;

    @AfterEach
    void tearDown() throws Exception {
        ThreadContext.clearAll();
    }

    @Test
    void testAdditionalFieldsWithLookup() throws Exception {
        putMdc("trace.id", "foo");
        putMdc("foo", "bar");
        putMdc(CUSTOM_MDC_SERIALIZER_TEST_KEY, "some_text_lower_case");
        debug("test");
        assertThat(getAndValidateLastLogLine().get("cluster.uuid").textValue()).isEqualTo("9fe9134b-20b0-465e-acf9-8cc09ac9053b");
        assertThat(getAndValidateLastLogLine().get("node.id").textValue()).isEqualTo("foo");
        assertThat(getAndValidateLastLogLine().get("empty")).isNull();
        assertThat(getAndValidateLastLogLine().get("emptyPattern")).isNull();
        assertThat(getAndValidateLastLogLine().get("clazz").textValue()).startsWith(getClass().getPackageName());
        assertThat(getAndValidateLastLogLine().get("404")).isNull();
        assertThat(getAndValidateLastLogLine().get("foo").textValue()).isEqualTo("bar");
        assertThat(getAndValidateLastLogLine().get(CUSTOM_MDC_SERIALIZER_TEST_KEY).textValue()).isEqualTo("some_text_lower_case");
    }

    @Test
    void testMarker() throws Exception {
        Marker parent = MarkerManager.getMarker("parent");
        Marker child = MarkerManager.getMarker("child").setParents(parent);
        Marker grandchild = MarkerManager.getMarker("grandchild").setParents(child);
        root.debug(grandchild, "test");

        assertThat(getAndValidateLastLogLine().get("tags")).contains(
                TextNode.valueOf("parent"),
                TextNode.valueOf("child"),
                TextNode.valueOf("grandchild"));
    }


    @Test
    void testCustomPatternConverter() throws Exception {
        debug("test");
        assertThat(getAndValidateLastLogLine().get("custom").textValue()).isEqualTo("foo");
    }

    @Test
    void testJsonMessageArray() throws Exception {
        root.info(new ObjectMessage(List.of("foo", "bar")));

        assertThat(getAndValidateLastLogLine().get("message").isArray()).isFalse();
        assertThat(getAndValidateLastLogLine().get("message").textValue()).contains("foo", "bar");
    }

    @Test
    void testJsonMessageString() throws Exception {
        root.info(new ObjectMessage("foo"));

        assertThat(getAndValidateLastLogLine().get("message").textValue()).isEqualTo("foo");
    }

    @Test
    void testJsonMessageNumber() throws Exception {
        root.info(new ObjectMessage(42));

        assertThat(getAndValidateLastLogLine().get("message").isNumber()).isFalse();
        assertThat(getAndValidateLastLogLine().get("message").textValue()).isEqualTo("42");
    }

    @Test
    void testJsonMessageBoolean() throws Exception {
        root.info(new ObjectMessage(true));

        assertThat(getAndValidateLastLogLine().get("message").isBoolean()).isFalse();
        assertThat(getAndValidateLastLogLine().get("message").textValue()).isEqualTo("true");
    }

    @Override
    public boolean putMdc(String key, String value) {
        ThreadContext.put(key, value);
        return true;
    }

    @Override
    public boolean putMdc(String message) {
        ThreadContext.push(message);
        return true;
    }

    @Override
    public void debug(String message) {
        root.debug(message);
    }

    @Override
    public void debug(String message, Object... logParams) {
        root.debug(message, logParams);
    }

    @Override
    public void error(String message, Throwable t) {
        root.error(message, t);
    }
}

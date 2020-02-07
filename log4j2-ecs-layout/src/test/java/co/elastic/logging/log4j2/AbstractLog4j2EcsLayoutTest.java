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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.StringMapMessage;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractLog4j2EcsLayoutTest extends AbstractEcsLoggingTest {
    protected Logger root;
    protected ListAppender listAppender;

    @AfterEach
    void tearDown() throws Exception {
        ThreadContext.clearAll();
    }

    @Test
    void globalLabels() throws Exception {
        putMdc("trace.id", "foo");
        putMdc("top_level", "foo");
        putMdc("nested_under_labels", "foo");
        debug("test");
        assertThat(getLastLogLine().get("cluster.uuid").textValue()).isEqualTo("9fe9134b-20b0-465e-acf9-8cc09ac9053b");
        assertThat(getLastLogLine().get("node.id").textValue()).isEqualTo("foo");
        assertThat(getLastLogLine().get("empty")).isNull();
        assertThat(getLastLogLine().get("emptyPattern")).isNull();
        assertThat(getLastLogLine().get("clazz").textValue()).startsWith(getClass().getPackageName());
        assertThat(getLastLogLine().get("404")).isNull();
        assertThat(getLastLogLine().get("top_level").textValue()).isEqualTo("foo");
        assertThat(getLastLogLine().get("labels.nested_under_labels").textValue()).isEqualTo("foo");
    }

    @Test
    void testMarker() throws Exception {
        Marker parent = MarkerManager.getMarker("parent");
        Marker child = MarkerManager.getMarker("child").setParents(parent);
        Marker grandchild = MarkerManager.getMarker("grandchild").setParents(child);
        root.debug(grandchild, "test");

        assertThat(getLastLogLine().get("tags")).contains(
                TextNode.valueOf("parent"),
                TextNode.valueOf("child"),
                TextNode.valueOf("grandchild"));
    }

    @Test
    void testMapMessage() throws Exception {
        root.info(new StringMapMessage().with("message", "foo").with("foo", "bar"));
        JsonNode log = getLastLogLine();
        assertThat(log.get("message").textValue()).isEqualTo("foo");
        assertThat(log.get("foo").textValue()).isEqualTo("bar");
    }

    @Test
    void testParameterizedStructuredMessage() throws Exception {
        root.info(ParameterizedStructuredMessage.of("hello {}", "world").with("foo", "bar"));
        assertThat(getLastLogLine().get("message").textValue()).isEqualTo("hello world");
        assertThat(getLastLogLine().get("foo").textValue()).isEqualTo("bar");
    }

    @Test
    void testCustomPatternConverter() throws Exception {
        debug("test");
        assertThat(getLastLogLine().get("custom").textValue()).isEqualTo("foo");
    }

    @Test
    void testJsonMessageObject() throws Exception {
        root.info(new ObjectMessage(new TestClass("foo", 42, true)));

        assertThat(getLastLogLine().get("foo").textValue()).isEqualTo("foo");
        assertThat(getLastLogLine().get("bar").intValue()).isEqualTo(42);
        assertThat(getLastLogLine().get("baz").booleanValue()).isEqualTo(true);
    }

    @Test
    void testJsonMessageArray() throws Exception {
        root.info(new ObjectMessage(List.of("foo", "bar")));

        assertThat(getLastLogLine().get("message").isArray()).isFalse();
        assertThat(getLastLogLine().get("message").textValue()).isEqualTo("[\"foo\",\"bar\"]");
    }

    @Test
    void testJsonMessageString() throws Exception {
        root.info(new ObjectMessage("foo"));

        assertThat(getLastLogLine().get("message").textValue()).isEqualTo("foo");
    }

    @Test
    void testJsonMessageNumber() throws Exception {
        root.info(new ObjectMessage(42));

        assertThat(getLastLogLine().get("message").isNumber()).isFalse();
        assertThat(getLastLogLine().get("message").textValue()).isEqualTo("42");
    }

    @Test
    void testJsonMessageBoolean() throws Exception {
        root.info(new ObjectMessage(true));

        assertThat(getLastLogLine().get("message").isBoolean()).isFalse();
        assertThat(getLastLogLine().get("message").textValue()).isEqualTo("true");
    }

    public static class TestClass {
        String foo;
        int bar;
        boolean baz;

        private TestClass() {
        }

        private TestClass(String foo, int bar, boolean baz) {
            this.foo = foo;
            this.bar = bar;
            this.baz = baz;
        }

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }

        public int getBar() {
            return bar;
        }

        public void setBar(int bar) {
            this.bar = bar;
        }

        public boolean isBaz() {
            return baz;
        }

        public void setBaz(boolean baz) {
            this.baz = baz;
        }
    }

    @Override
    public void putMdc(String key, String value) {
        ThreadContext.put(key, value);
    }

    @Override
    public boolean putNdc(String message) {
        ThreadContext.push(message);
        return true;
    }

    @Override
    public void debug(String message) {
        root.debug(message);
    }

    @Override
    public void error(String message, Throwable t) {
        root.error(message, t);
    }
}

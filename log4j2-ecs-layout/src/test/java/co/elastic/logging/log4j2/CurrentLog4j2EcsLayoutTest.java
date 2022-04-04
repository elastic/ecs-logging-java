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

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.StringMapMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CurrentLog4j2EcsLayoutTest extends Log4j2EcsLayoutTest {
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
    void testJsonMessageObject() throws Exception {
        root.info(new ObjectMessage(new TestClass("foo", 42, true)));

        assertThat(getLastLogLine().get("foo").textValue()).isEqualTo("foo");
        assertThat(getLastLogLine().get("bar").intValue()).isEqualTo(42);
        assertThat(getLastLogLine().get("baz").booleanValue()).isEqualTo(true);
    }

    @Test
    void testExceptionPattern() throws Exception {
        error("test", new RuntimeException("test"));
        JsonNode log = getAndValidateLastLogLine();
        assertThat(log.get("log.level").textValue()).isIn("ERROR", "SEVERE");
        assertThat(log.get("error.message").textValue()).isEqualTo("test");
        assertThat(log.get("error.type").textValue()).isEqualTo(RuntimeException.class.getName());
        assertThat(log.get("error.stack_trace").textValue().split("\\n").length).isEqualTo(4L);
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
}

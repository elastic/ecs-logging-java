/*-
 * #%L
 * Java ECS logging
 * %%
 * Copyright (C) 2019 - 2022 Elastic and contributors
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EcsLayoutWithStackTraceAsArrayTest extends Log4j2EcsLayoutTest {

    @Override
    protected EcsLayout.Builder configureLayout(LoggerContext context) {
        return super.configureLayout(context)
                .setExceptionPattern("%rEx{4,filters(java.base,java.lang)}")
                .setStackTraceAsArray(true);
    }

    @Test
    void testLogException() throws Exception {
        error("test", numberFormatException());
        JsonNode log = getLastLogLine();
        assertThat(log.get("log.level").textValue()).isIn("ERROR", "SEVERE");
        assertThat(log.get("error.message").textValue()).isEqualTo("For input string: \"NOT_AN_INT\"");
        assertThat(log.get("error.type").textValue()).isEqualTo(NumberFormatException.class.getName());
        assertThat(log.get("error.stack_trace").isArray()).isTrue();
        ArrayNode arrayNode = (ArrayNode) log.get("error.stack_trace");
        assertThat(arrayNode.size()).isEqualTo(4);
        assertThat(arrayNode.get(0).textValue()).isEqualTo("java.lang.NumberFormatException: For input string: \"NOT_AN_INT\"");
        assertThat(arrayNode.get(1).textValue()).startsWith("\t... suppressed");
        assertThat(arrayNode.get(2).textValue()).startsWith("\tat co.elastic.logging.log4j2.EcsLayoutWithStackTraceAsArrayTest.numberFormatException");
        assertThat(arrayNode.get(3).textValue()).startsWith("\tat co.elastic.logging.log4j2.EcsLayoutWithStackTraceAsArrayTest.testLogException");
    }

    private static Throwable numberFormatException() {
        try {
            Integer.parseInt("NOT_AN_INT");
            return null;
        } catch (Exception ex) {
            return ex;
        }
    }

    @Test
    void testLogExceptionNullMessage() throws Exception {
        error("test", new RuntimeException());
        // skip validation that error.stack_trace is a string
        JsonNode log = getLastLogLine();
        assertThat(log.get("error.type").textValue()).isEqualTo(RuntimeException.class.getName());
        assertThat(log.get("error.message")).isNull();
        assertThat(log.get("error.stack_trace").isArray()).isTrue();
        ArrayNode arrayNode = (ArrayNode) log.get("error.stack_trace");
        assertThat(arrayNode.size()).isEqualTo(4);
        assertThat(arrayNode.get(0).textValue()).isEqualTo("java.lang.RuntimeException");
        assertThat(arrayNode.get(1).textValue()).startsWith("\tat co.elastic.logging.log4j2.EcsLayoutWithStackTraceAsArrayTest.testLogExceptionNullMessage");
    }
}

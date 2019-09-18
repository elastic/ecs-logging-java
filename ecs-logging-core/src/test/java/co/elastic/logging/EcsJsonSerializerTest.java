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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class EcsJsonSerializerTest {

    @Test
    void serializeExceptionAsString() throws IOException {
        Exception exception = new Exception("foo");
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append('{');
        EcsJsonSerializer.serializeException(jsonBuilder, exception, false);
        jsonBuilder.append('}');
        JsonNode jsonNode = new ObjectMapper().readTree(jsonBuilder.toString());

        assertThat(jsonNode.get("error.code").textValue()).isEqualTo(exception.getClass().getName());
        assertThat(jsonNode.get("error.message").textValue()).isEqualTo("foo");
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        assertThat(jsonNode.get("error.stack_trace").textValue()).isEqualTo(stringWriter.toString());
    }

    @Test
    void serializeExceptionAsArray() throws IOException {
        Exception exception = new Exception("foo");
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append('{');
        EcsJsonSerializer.serializeException(jsonBuilder, exception, true);
        jsonBuilder.append('}');
        System.out.println(jsonBuilder);
        JsonNode jsonNode = new ObjectMapper().readTree(jsonBuilder.toString());

        assertThat(jsonNode.get("error.code").textValue()).isEqualTo(exception.getClass().getName());
        assertThat(jsonNode.get("error.message").textValue()).isEqualTo("foo");
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        assertThat(StreamSupport.stream(jsonNode.get("error.stack_trace").spliterator(), false)
                .map(JsonNode::textValue)
                .collect(Collectors.joining("\n", "", "\n")))
                .isEqualTo(stringWriter.toString());
    }

    @Test
    void testRemoveIfEndsWith() {
        assertRemoveIfEndsWith("", "foo", "");
        assertRemoveIfEndsWith("foobar", "foo", "foobar");
        assertRemoveIfEndsWith("barfoo", "foo", "bar");
    }

    private void assertRemoveIfEndsWith(String builder, String ending, String expected) {
        StringBuilder sb = new StringBuilder(builder);
        EcsJsonSerializer.removeIfEndsWith(sb, ending);
        assertThat(sb.toString()).isEqualTo(expected);
    }
}

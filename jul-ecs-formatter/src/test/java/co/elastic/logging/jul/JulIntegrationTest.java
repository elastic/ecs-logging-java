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
package co.elastic.logging.jul;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.logging.LogManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests logging setup based on a custom logging.properties file, as well as Exception stack trace formatting as array
 */
public class JulIntegrationTest extends JulLoggingTest {
    @BeforeEach
    void setUp() throws IOException {
        LogManager.getLogManager().readConfiguration(getClass().getClassLoader().getResourceAsStream("logging.properties"));
        super.setUpFormatter();
    }

    @Override
    protected EcsFormatter createEcsFormatter() {
        // relying on the configuration loaded from the logging.properties file
        return new EcsFormatter();
    }

    @Test
    void testLogException() throws Exception {
        error("test", new RuntimeException("test"));
        JsonNode log = getLastLogLine();
        assertThat(log.get("log.level").textValue()).isIn("ERROR", "SEVERE");
        assertThat(log.get("error.message").textValue()).isEqualTo("test");
        assertThat(log.get("error.type").textValue()).isEqualTo(RuntimeException.class.getName());
        assertThat(log.get("error.stack_trace").isArray()).isTrue();
        assertThat(log.get("error.stack_trace").get(0).textValue()).isEqualTo("java.lang.RuntimeException: test");
        assertThat(log.get("error.stack_trace").get(1).textValue()).contains("at co.elastic.logging.jul.JulIntegrationTest");
    }

    @Test
    void testLogExceptionNullMessage() throws Exception {
        error("test", new RuntimeException());
        JsonNode log = getLastLogLine();
        assertThat(log.get("error.message")).isNull();
        assertThat(log.get("error.type").textValue()).isEqualTo(RuntimeException.class.getName());
        assertThat(log.get("error.stack_trace").get(0).textValue()).isEqualTo("java.lang.RuntimeException");
        assertThat(log.get("error.stack_trace").get(1).textValue()).contains("at co.elastic.logging.jul.JulIntegrationTest");
    }
}

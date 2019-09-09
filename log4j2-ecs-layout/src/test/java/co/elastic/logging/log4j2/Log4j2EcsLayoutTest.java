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
package co.elastic.logging.log4j2;

import co.elastic.logging.AbstractEcsLoggingTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.BasicConfigurationFactory;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.StringMapMessage;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class Log4j2EcsLayoutTest extends AbstractEcsLoggingTest {

    private static ConfigurationFactory configFactory = new BasicConfigurationFactory();
    private LoggerContext ctx = LoggerContext.getContext();
    private Logger root = ctx.getRootLogger();
    private ObjectMapper objectMapper = new ObjectMapper();
    private ListAppender listAppender;

    @AfterAll
    public static void cleanupClass() {
        ConfigurationFactory.removeConfigurationFactory(configFactory);
    }

    @BeforeAll
    public static void setupClass() {
        ConfigurationFactory.setConfigurationFactory(configFactory);
        final LoggerContext ctx = LoggerContext.getContext();
        ctx.reconfigure();
    }

    @BeforeEach
    public void setUp() throws Exception {
        for (final Appender appender : root.getAppenders().values()) {
            root.removeAppender(appender);
        }
        EcsLayout ecsLayout = EcsLayout.newBuilder()
                .setConfiguration(ctx.getConfiguration())
                .setServiceName("test")
                .setAdditionalFields(new KeyValuePair[]{
                        new KeyValuePair("cluster.uuid", "9fe9134b-20b0-465e-acf9-8cc09ac9053b"),
                        new KeyValuePair("node.id", "${node.id}"),
                })
                .build();

        listAppender = new ListAppender("ecs", null, ecsLayout, false, false);
        listAppender.start();
        root.addAppender(listAppender);
        root.setLevel(Level.DEBUG);
        ctx.getConfiguration().getProperties().put("node.id", "foo");
    }

    @AfterEach
    public void tearDown() throws Exception {
        ThreadContext.clearAll();
    }

    @Test
    void globalLabels() throws Exception {
        putMdc("trace.id", "foo");
        debug("test");
        assertThat(getLastLogLine().get("cluster.uuid").textValue()).isEqualTo("9fe9134b-20b0-465e-acf9-8cc09ac9053b");
        assertThat(getLastLogLine().get("node.id").textValue()).isEqualTo("foo");
        assertThat(getLastLogLine().get("404")).isNull();
    }

    @Test
    void testMapMessage() throws Exception {
        root.info(new StringMapMessage(Map.of("foo", "bar")));
        assertThat(getLastLogLine().get("labels.foo").textValue()).isEqualTo("bar");
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

    @Override
    public JsonNode getLastLogLine() throws IOException {
        String content = listAppender.getMessages().get(0);
        System.out.println(content);
        return objectMapper.readTree(content);
    }
}

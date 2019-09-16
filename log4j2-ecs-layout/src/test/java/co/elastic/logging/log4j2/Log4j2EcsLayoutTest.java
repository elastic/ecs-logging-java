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

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.BasicConfigurationFactory;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

class Log4j2EcsLayoutTest extends AbstractLog4j2EcsLayoutTest {
    private static ConfigurationFactory configFactory = new BasicConfigurationFactory();
    private LoggerContext ctx;

    @AfterAll
    static void cleanupClass() {
        ConfigurationFactory.removeConfigurationFactory(configFactory);
    }

    @BeforeAll
    static void setupClass() {
        ConfigurationFactory.setConfigurationFactory(configFactory);
    }

    @BeforeEach
    void setUp() {
        ctx = new LoggerContext("Test");
        ctx.reconfigure();
        ctx.getConfiguration().getProperties().put("node.id", "foo");

        root = ctx.getRootLogger();

        for (final Appender appender : root.getAppenders().values()) {
            root.removeAppender(appender);
        }
        EcsLayout ecsLayout = EcsLayout.newBuilder()
                .setConfiguration(ctx.getConfiguration())
                .setServiceName("test")
                .setIncludeMarkers(true)
                .setStackTraceAsArray(true)
                .setAdditionalFields(new KeyValuePair[]{
                        new KeyValuePair("cluster.uuid", "9fe9134b-20b0-465e-acf9-8cc09ac9053b"),
                        new KeyValuePair("node.id", "${node.id}"),
                        new KeyValuePair("empty", "${empty}"),
                })
                .build();

        listAppender = new ListAppender("ecs", null, ecsLayout, false, false);
        listAppender.start();
        root.addAppender(listAppender);
        root.setLevel(Level.DEBUG);
    }

    @AfterEach
    @Override
    void tearDown() throws Exception {
        super.tearDown();
        ctx.close();
    }

    @Override
    public JsonNode getLastLogLine() throws IOException {
        return objectMapper.readTree(listAppender.getMessages().get(0));
    }
}

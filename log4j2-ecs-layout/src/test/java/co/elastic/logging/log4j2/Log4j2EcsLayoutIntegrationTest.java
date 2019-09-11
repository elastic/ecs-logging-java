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
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.net.URISyntaxException;

class Log4j2EcsLayoutIntegrationTest extends AbstractLog4j2EcsLayoutTest {

    private static ConfigurationFactory configFactory = new XmlConfigurationFactory();
    private LoggerContext ctx = LoggerContext.getContext();
    private Logger root = ctx.getRootLogger();
    private ListAppender listAppender;

    @AfterAll
    static void cleanupClass() {
        ConfigurationFactory.removeConfigurationFactory(configFactory);
    }

    @BeforeAll
    static void setupClass() throws URISyntaxException {
        ConfigurationFactory.setConfigurationFactory(configFactory);
        final ClassLoader classLoader = Log4j2EcsLayoutIntegrationTest.class.getClassLoader();
        final LoggerContext ctx = LoggerContext.getContext(classLoader,
                true,
                classLoader.getResource("log4j2-config.xml").toURI());
        ctx.reconfigure();
    }

    @BeforeEach
    void setUp() {
        listAppender = (ListAppender) root.getAppenders().get("TestAppender");
        ctx.getConfiguration().getProperties().put("node.id", "foo");
    }

    @AfterEach
    void tearDown() throws Exception {
        super.tearDown();
        listAppender.clear();
    }

    @Override
    public JsonNode getLastLogLine() throws IOException {
        return objectMapper.readTree(listAppender.getMessages().get(0));
    }
}

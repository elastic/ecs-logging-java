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
package co.elastic.logging.logback;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import co.elastic.logging.AbstractEcsLoggingTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class EcsEncoderIntegrationTest extends AbstractEcsLoggingTest {
    private OutputStreamAppender appender;
    private Logger logger;

    @BeforeEach
    void setUp() throws JoranException {
        LoggerContext context = new LoggerContext();
        ContextInitializer contextInitializer = new ContextInitializer(context);
        contextInitializer.configureByResource(this.getClass().getResource("/logback-config.xml"));
        logger = context.getLogger("root");
        appender = (OutputStreamAppender) logger.getAppender("out");
    }

    @Override
    public void putMdc(String key, String value) {
        MDC.put(key, value);
    }

    @Override
    public void debug(String message) {
        logger.debug(message);
    }

    @Test
    void testMarker() throws Exception {
        Marker parent = MarkerFactory.getMarker("parent");
        Marker child = MarkerFactory.getMarker("child");
        Marker grandchild = MarkerFactory.getMarker("grandchild");
        child.add(grandchild);
        parent.add(child);
        logger.debug(parent, "test");

        assertThat(getLastLogLine().get("tags")).contains(
                TextNode.valueOf("parent"),
                TextNode.valueOf("child"),
                TextNode.valueOf("grandchild"));
    }

    @Override
    public void error(String message, Throwable t) {
        logger.error(message, t);
    }

    @Override
    public JsonNode getLastLogLine() throws IOException {
        return objectMapper.readTree(appender.getBytes());
    }
}

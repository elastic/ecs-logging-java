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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

class EcsEncoderTest extends AbstractEcsEncoderTest {

    private ListAppender<ILoggingEvent> appender;
    private EcsEncoder ecsEncoder;

    @BeforeEach
    void setUp() {
        LoggerContext context = new LoggerContext();
        logger = context.getLogger(getClass());
        appender = new ListAppender<>();
        appender.setContext(context);
        appender.start();
        logger.addAppender(appender);
        ecsEncoder = new EcsEncoder();
        ecsEncoder.setServiceName("test");
        ecsEncoder.setIncludeMarkers(true);
        ecsEncoder.setStackTraceAsArray(true);
        ecsEncoder.start();
    }

    @Override
    public JsonNode getLastLogLine() throws IOException {
        return objectMapper.readTree(ecsEncoder.encode(appender.list.get(0)));
    }
}

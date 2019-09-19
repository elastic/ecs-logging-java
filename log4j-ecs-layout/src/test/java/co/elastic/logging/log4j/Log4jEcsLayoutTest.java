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
package co.elastic.logging.log4j;

import co.elastic.logging.AbstractEcsLoggingTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.NDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

class Log4jEcsLayoutTest extends AbstractEcsLoggingTest {

    private Logger logger;
    private ListAppender appender;
    private EcsLayout ecsLayout;

    @BeforeEach
    void setUp() {
        logger = LogManager.getLogger(getClass());
        logger.removeAllAppenders();
        appender = new ListAppender();
        logger.addAppender(appender);
        ecsLayout = new EcsLayout();
        ecsLayout.setServiceName("test");
        ecsLayout.setStackTraceAsArray(true);
        ecsLayout.setIncludeOrigin(true);
        appender.setLayout(ecsLayout);
    }

    @BeforeEach
    @AfterEach
    void tearDown() {
        MDC.clear();
        NDC.clear();
    }

    @Override
    public void putMdc(String key, String value) {
        MDC.put(key, value);
        Assumptions.assumeTrue(value.equals(MDC.get(key)));
    }

    @Override
    public boolean putNdc(String message) {
        NDC.push(message);
        return true;
    }

    @Override
    public void debug(String message) {
        logger.debug(message);
    }

    @Override
    public void error(String message, Throwable t) {
        logger.error(message, t);
    }

    @Override
    public JsonNode getLastLogLine() throws IOException {
        return objectMapper.readTree(appender.getLogEvents().get(0));
    }

}

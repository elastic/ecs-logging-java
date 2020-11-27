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
package co.elastic.logging.log4j;

import co.elastic.logging.AbstractEcsLoggingTest;
import co.elastic.logging.ParameterizedLogSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.NDC;
import org.apache.log4j.spi.LoggerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class Log4jEcsLayoutIntegrationTest extends AbstractEcsLoggingTest {

    private Logger logger;
    private ListAppender appender;

    @BeforeEach
    void setUp() {
        logger = LogManager.getLogger(getClass());
        appender = (ListAppender) logger.getParent().getAppender("List");
    }

    @AfterEach
    void tearDown() {
        try {
            // available since 1.2.16
            MDC.class.getMethod("clear").invoke(null);
        } catch (Exception ignore) {
        }
        NDC.clear();
        appender.getLogEvents().clear();
    }

    @Test
    void testAdditionalField() throws Exception {
        debug("test");
        assertThat(getAndValidateLastLogLine().get("additionalField1").textValue()).isEqualTo("value1");
        assertThat(getAndValidateLastLogLine().get("additionalField2").textValue()).isEqualTo("value2");
    }

    @Override
    public boolean putMdc(String key, String value) {
        MDC.put(key, value);
        Assumptions.assumeTrue(value.equals(MDC.get(key)));
        return true;
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
    public ParameterizedLogSupport getParameterizedLogSettings() {
        return ParameterizedLogSupport.NOT_SUPPORTED;
    }

    @Override
    public void debug(String message, Object... logParams) {
        throw new UnsupportedOperationException();
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

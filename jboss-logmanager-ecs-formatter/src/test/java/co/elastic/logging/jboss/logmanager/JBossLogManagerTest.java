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
package co.elastic.logging.jboss.logmanager;

import co.elastic.logging.AbstractEcsLoggingTest;
import co.elastic.logging.ParameterizedLogSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.Logger;
import org.jboss.logmanager.JBossLogmanagerLoggerProducer;
import org.jboss.logmanager.MDC;
import org.jboss.logmanager.NDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

class JBossLogManagerTest extends AbstractEcsLoggingTest {

    private final EcsFormatter formatter = new EcsFormatter();

    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    private final Logger logger = JBossLogmanagerLoggerProducer.getLogger("JBossLogManagerTest");

    @Override
    public boolean putMdc(String key, String value) {
        MDC.put(key, value);
        return true;
    }

    @Override
    public boolean putNdc(String message) {
        NDC.push(message);
        return true;
    }

    @Override
    public void debug(String message) {
        logger.log(Level.DEBUG, message);
    }

    @Override
    public ParameterizedLogSupport getParameterizedLogSettings() {
        return ParameterizedLogSupport.NUMBER_AND_BRACKETS;
    }

    @Override
    public void debug(String message, Object... logParams) {
        logger.log(Level.DEBUG, message, logParams);
    }

    @Override
    public void error(String message, Throwable t) {
        logger.log(Level.ERROR, message, t);
    }

    @Override
    public JsonNode getLastLogLine() throws IOException {
        return objectMapper.readTree(byteArrayOutputStream.toString());
    }

    @BeforeEach
    void setUp() {
        formatter.setIncludeOrigin(true);
        formatter.setServiceName("test");
        formatter.setEventDataset("testdataset.log");
        formatter.setAdditionalFields("key1=value1,key2=value2");

        logger.setLevel(Level.ALL);
        logger.addHandler(new StreamHandler(byteArrayOutputStream, formatter) {

            @Override
            public boolean isLoggable(LogRecord record) {
                return true;
            }

            @Override
            public void publish(LogRecord record) {
                super.publish(record);
                flush();
            }
        });

        MDC.clear();
        NDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        NDC.clear();
    }
}

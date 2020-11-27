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
package co.elastic.logging.jul;

import co.elastic.logging.AbstractEcsLoggingTest;
import co.elastic.logging.ParameterizedLogSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

public class JulLoggingTest extends AbstractEcsLoggingTest {

    private static final class InMemoryStreamHandler extends StreamHandler {
        private InMemoryStreamHandler(OutputStream out, Formatter formatter) {
            super(out, formatter);
        }

        /**
         * Override {@code StreamHandler.close} to do a flush but not
         * to close the output stream.  That is, we do <b>not</b>
         * close {@code System.err}.
         */
        @Override
        public void close() {
            flush();
        }

        @Override
        public void publish(LogRecord record) {
            super.publish(record);
            flush();
        }
    }

    private final EcsFormatter formatter = new EcsFormatter();
    
    private final Logger logger = Logger.getLogger("");
    
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    private LogRecord record;

    @Override
    public void debug(String message) {
        logger.log(Level.FINE, message);
    }

    @Override
    public boolean putMdc(String key, String value) {
        MDC.put(key, value);
        return true;
    }

    @Override
    public ParameterizedLogSupport getParameterizedLogSettings() {
        return ParameterizedLogSupport.NUMBER_AND_BRACKETS;
    }

    @Override
    public void debug(String message, Object... logParams) {
        logger.log(Level.FINE, message, logParams);
    }

    @Override
    public void error(String message, Throwable t) {
        logger.log(Level.SEVERE, message, t);
    }
    
    @Override
    public JsonNode getLastLogLine() throws IOException {
        return objectMapper.readTree(out.toString());
    }

    @BeforeEach
    void setUp() {
        clearHandlers();
        
        formatter.setIncludeOrigin(true);
        formatter.setStackTraceAsArray(true);
        formatter.setServiceName("test");
        formatter.setEventDataset("testdataset.log");
        formatter.setAdditionalFields("key1=value1,key2=value2");
        
        Handler handler = new InMemoryStreamHandler(out, formatter);
        handler.setLevel(Level.ALL);
        
        logger.addHandler(handler);
        logger.setLevel(Level.ALL);
    }

    @Test
    void testLogException() throws Exception {
        error("test", new RuntimeException("test"));
        assertThat(getAndValidateLastLogLine().get("log.level").textValue()).isEqualTo("SEVERE");
        assertThat(getAndValidateLastLogLine().get("error.message").textValue()).isEqualTo("test");
        assertThat(getAndValidateLastLogLine().get("error.type").textValue()).isEqualTo(RuntimeException.class.getName());
        String stackTrace = StreamSupport.stream(getAndValidateLastLogLine().get("error.stack_trace").spliterator(), false)
                .map(JsonNode::textValue)
                .collect(Collectors.joining("\n", "", "\n"));
        assertThat(stackTrace).contains("at co.elastic.logging.jul.JulLoggingTest.testLogException");
    }

    @Test
    void testLogOrigin() throws Exception {
        debug("test");
        assertThat(getAndValidateLastLogLine().get("log.origin").get("file.name").textValue()).endsWith(".java");
        assertThat(getAndValidateLastLogLine().get("log.origin").get("function").textValue()).isEqualTo("debug");
        //No file.line for JUL
    }

    
    private void clearHandlers() {
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }
    }

}

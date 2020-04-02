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

import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import co.elastic.logging.EcsJsonSerializer;

public class EcsFormatter extends Formatter {

    private boolean stackTraceAsArray;
    private String serviceName;
    private boolean includeOrigin;
    private String eventDataset;

    /**
     * Default constructor. Will read configuration from LogManager properties.
     */
    public EcsFormatter() {
        serviceName = getProperty("co.elastic.logging.jul.EcsFormatter.serviceName", null);
        includeOrigin = Boolean.getBoolean(getProperty("co.elastic.logging.jul.EcsFormatter.includeOrigin", "false"));
        stackTraceAsArray = Boolean
                .getBoolean(getProperty("co.elastic.logging.jul.EcsFormatter.stackTraceAsArray", "false"));
        eventDataset = getProperty("co.elastic.logging.jul.EcsFormatter.eventDataset", null);
        eventDataset = EcsJsonSerializer.computeEventDataset(eventDataset, serviceName);
    }

    @Override
    public String format(final LogRecord record) {
        final StringBuilder builder = new StringBuilder();
        EcsJsonSerializer.serializeObjectStart(builder, record.getMillis());
        EcsJsonSerializer.serializeLogLevel(builder, record.getLevel().getName());
        EcsJsonSerializer.serializeFormattedMessage(builder, record.getMessage());
        EcsJsonSerializer.serializeServiceName(builder, serviceName);
        EcsJsonSerializer.serializeEventDataset(builder, eventDataset);
        EcsJsonSerializer.serializeThreadName(builder, "thread-" + record.getThreadID());
        EcsJsonSerializer.serializeThreadId(builder, record.getThreadID());
        EcsJsonSerializer.serializeLoggerName(builder, record.getSourceClassName());
        if (includeOrigin && record.getSourceClassName() != null && record.getSourceMethodName() != null) {
            EcsJsonSerializer.serializeOrigin(builder, record.getSourceClassName().replaceAll(".*\\.", "") + ".java",
                    record.getSourceMethodName(), 1);

            // EcsJsonSerializer.serializeOrigin(builder, callerData[0]);
        }
        final Throwable throwableInformation = record.getThrown();
        if (throwableInformation != null) {
            EcsJsonSerializer.serializeException(builder, throwableInformation, stackTraceAsArray);
        }
        EcsJsonSerializer.serializeObjectEnd(builder);
        return builder.toString();
    }

    protected void setIncludeOrigin(final boolean includeOrigin) {
        this.includeOrigin = includeOrigin;
    }

    protected void setServiceName(final String serviceName) {
        this.serviceName = serviceName;
    }

    protected void setStackTraceAsArray(final boolean stackTraceAsArray) {
        this.stackTraceAsArray = stackTraceAsArray;
    }
    
    public void setEventDataset(String eventDataset) {
        this.eventDataset = eventDataset;
    }

    private String getProperty(final String name, final String defaultValue) {
        String value = LogManager.getLogManager().getProperty(name);
        if (value == null) {
            value = defaultValue;
        } else {
            value = value.trim();
        }
        return value;
    }

}

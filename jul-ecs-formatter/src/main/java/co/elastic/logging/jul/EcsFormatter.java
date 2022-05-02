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

import java.util.Collections;
import java.util.List;
import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import co.elastic.logging.AdditionalField;
import co.elastic.logging.EcsJsonSerializer;

public class EcsFormatter extends Formatter {

    private static final String UNKNOWN_FILE = "<Unknown>";
    private static final MdcSupplier mdcSupplier = MdcSupplier.Resolver.INSTANCE.resolve();
    
    private boolean stackTraceAsArray;
    private String serviceName;
    private String serviceVersion;
    private String serviceEnvironment;
    private String serviceNodeName;
    private boolean includeOrigin;
    private String eventDataset;
    private List<AdditionalField> additionalFields = Collections.emptyList();

    /**
     * Default constructor. Will read configuration from LogManager properties.
     */
    public EcsFormatter() {
        serviceName = getProperty("co.elastic.logging.jul.EcsFormatter.serviceName", null);
        serviceVersion= getProperty("co.elastic.logging.jul.EcsFormatter.serviceVersion", null);
        serviceEnvironment= getProperty("co.elastic.logging.jul.EcsFormatter.serviceEnvironment", null);
        serviceNodeName = getProperty("co.elastic.logging.jul.EcsFormatter.serviceNodeName", null);
        includeOrigin = Boolean.parseBoolean(getProperty("co.elastic.logging.jul.EcsFormatter.includeOrigin", "false"));
        stackTraceAsArray = Boolean.parseBoolean(getProperty("co.elastic.logging.jul.EcsFormatter.stackTraceAsArray", "false"));
        eventDataset = getProperty("co.elastic.logging.jul.EcsFormatter.eventDataset", null);
        eventDataset = EcsJsonSerializer.computeEventDataset(eventDataset, serviceName);
        setAdditionalFields(getProperty("co.elastic.logging.jul.EcsFormatter.additionalFields", null));
    }

    @Override
    public String format(final LogRecord record) {
        final StringBuilder builder = new StringBuilder();
        EcsJsonSerializer.serializeObjectStart(builder, record.getMillis());
        EcsJsonSerializer.serializeLogLevel(builder, record.getLevel().getName());
        EcsJsonSerializer.serializeFormattedMessage(builder, super.formatMessage(record));
        EcsJsonSerializer.serializeEcsVersion(builder);
        EcsJsonSerializer.serializeAdditionalFields(builder, additionalFields);
        EcsJsonSerializer.serializeMDC(builder, mdcSupplier.getMDC());
        EcsJsonSerializer.serializeServiceName(builder, serviceName);
        EcsJsonSerializer.serializeServiceVersion(builder, serviceVersion);
        EcsJsonSerializer.serializeServiceEnvironment(builder, serviceEnvironment);
        EcsJsonSerializer.serializeServiceNodeName(builder, serviceNodeName);
        EcsJsonSerializer.serializeEventDataset(builder, eventDataset);
        if (Thread.currentThread().getId() == record.getThreadID()) {
            EcsJsonSerializer.serializeThreadName(builder, Thread.currentThread().getName());
        } else {
            EcsJsonSerializer.serializeThreadId(builder, record.getThreadID());
        }
        EcsJsonSerializer.serializeLoggerName(builder, record.getLoggerName());
        if (includeOrigin && record.getSourceClassName() != null && record.getSourceMethodName() != null) {
            EcsJsonSerializer.serializeOrigin(builder, buildFileName(record.getSourceClassName()), record.getSourceMethodName(), -1);
        }
        final Throwable throwableInformation = record.getThrown();
        if (throwableInformation != null) {
            EcsJsonSerializer.serializeException(builder, throwableInformation, stackTraceAsArray);
        }
        EcsJsonSerializer.serializeObjectEnd(builder);
        return builder.toString();
    }

    public void setIncludeOrigin(final boolean includeOrigin) {
        this.includeOrigin = includeOrigin;
    }

    public void setServiceName(final String serviceName) {
        this.serviceName = serviceName;
    }

    public void setServiceVersion(final String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public void setServiceEnvironment(final String serviceEnvironment) {
        this.serviceEnvironment = serviceEnvironment;
    }

    public void setServiceNodeName(final String serviceNodeName) {
        this.serviceNodeName = serviceNodeName;
    }

    public void setStackTraceAsArray(final boolean stackTraceAsArray) {
        this.stackTraceAsArray = stackTraceAsArray;
    }
    
    public void setEventDataset(String eventDataset) {
        this.eventDataset = eventDataset;
    }

    public void setAdditionalFields(String additionalFields) {
        this.additionalFields = AdditionalField.parse(additionalFields);
    }

    public void setAdditionalFields(List<AdditionalField> additionalFields) {
        this.additionalFields = additionalFields;
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

    private String buildFileName(String className) {
        String result = UNKNOWN_FILE;
        if (className != null) {
            int fileNameEnd = className.indexOf('$');
            if (fileNameEnd < 0) {
                fileNameEnd = className.length();
            }
            int classNameStart = className.lastIndexOf('.');
            if (classNameStart < fileNameEnd) {
                result = className.substring(classNameStart + 1, fileNameEnd) + ".java";
            }
        }
        return result;
    }
}

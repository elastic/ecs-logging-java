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
import co.elastic.logging.DataStreamFieldSanitizer;
import co.elastic.logging.EcsJsonSerializer;

public class EcsFormatter extends Formatter {

    private static final String UNKNOWN_FILE = "<Unknown>";
    private static final MdcSupplier mdcSupplier = MdcSupplier.Resolver.INSTANCE.resolve();
    
    private boolean stackTraceAsArray;
    private String serviceName;
    private boolean includeOrigin;
    private String dataset;
    private String dataStreamNamespace;
    private List<AdditionalField> additionalFields = Collections.emptyList();

    /**
     * Default constructor. Will read configuration from LogManager properties.
     */
    public EcsFormatter() {
        setServiceName(getProperty("co.elastic.logging.jul.EcsFormatter.serviceName", null));
        includeOrigin = Boolean.getBoolean(getProperty("co.elastic.logging.jul.EcsFormatter.includeOrigin", "false"));
        stackTraceAsArray = Boolean
                .getBoolean(getProperty("co.elastic.logging.jul.EcsFormatter.stackTraceAsArray", "false"));
        String dataset = getProperty("co.elastic.logging.jul.EcsFormatter.dataStreamDataset", null);
        if (dataset == null) {
            dataset = getProperty("co.elastic.logging.jul.EcsFormatter.eventDataset", null);
        }
        setDataset(dataset);
        setDataStreamNamespace(getProperty("co.elastic.logging.jul.EcsFormatter.dataStreamNamespace", null));
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
        EcsJsonSerializer.serializeDataset(builder, dataset);
        EcsJsonSerializer.serializeNamespace(builder, dataStreamNamespace);
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

    protected void setIncludeOrigin(final boolean includeOrigin) {
        this.includeOrigin = includeOrigin;
    }

    protected void setServiceName(final String serviceName) {
        this.serviceName = serviceName;
        setDataset(EcsJsonSerializer.computeDataset(dataset, serviceName));
    }

    protected void setStackTraceAsArray(final boolean stackTraceAsArray) {
        this.stackTraceAsArray = stackTraceAsArray;
    }
    
    public void setEventDataset(String eventDataset) {
        setDataset(eventDataset);
    }

    public void setDataStreamDataset(String eventDataset) {
        setDataset(eventDataset);
    }

    private void setDataset(String dataset) {
        if (dataset != null) {
            this.dataset = DataStreamFieldSanitizer.sanitizeDataStreamDataset(dataset);
        }
    }

    public void setDataStreamNamespace(String dataStreamNamespace) {
        this.dataStreamNamespace = DataStreamFieldSanitizer.sanitizeDataStreamNamespace(dataStreamNamespace);
    }

    public void setAdditionalFields(String additionalFields) {
        this.additionalFields = AdditionalField.parse(additionalFields);
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

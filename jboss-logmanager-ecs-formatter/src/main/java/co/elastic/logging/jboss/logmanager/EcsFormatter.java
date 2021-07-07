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

import co.elastic.logging.EcsJsonSerializer;
import co.elastic.logging.AdditionalField;
import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.LogManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EcsFormatter extends ExtFormatter {

    private String serviceName;
    private String serviceNodeName;
    private String eventDataset;
    private List<AdditionalField> additionalFields = Collections.emptyList();
    private boolean includeOrigin;
    private boolean stackTraceAsArray;

    public EcsFormatter() {
        serviceName = getProperty("co.elastic.logging.jboss.logmanager.EcsFormatter.serviceName", null);
        serviceNodeName = getProperty("co.elastic.logging.jboss.logmanager.EcsFormatter.serviceNodeName", null);
        eventDataset = getProperty("co.elastic.logging.jboss.logmanager.EcsFormatter.eventDataset", null);
        eventDataset = EcsJsonSerializer.computeEventDataset(eventDataset, serviceName);
        includeOrigin = Boolean.getBoolean(getProperty("co.elastic.logging.jboss.logmanager.EcsFormatter.includeOrigin", "false"));
        stackTraceAsArray = Boolean.getBoolean(getProperty("co.elastic.logging.jboss.logmanager.EcsFormatter.stackTraceAsArray", "false"));
    }

    @Override
    public String format(ExtLogRecord record) {
        StringBuilder builder = new StringBuilder();
        EcsJsonSerializer.serializeObjectStart(builder, record.getMillis());
        EcsJsonSerializer.serializeLogLevel(builder, record.getLevel().getName());
        EcsJsonSerializer.serializeFormattedMessage(builder, record.getFormattedMessage());
        EcsJsonSerializer.serializeEcsVersion(builder);
        EcsJsonSerializer.serializeServiceName(builder, serviceName);
        EcsJsonSerializer.serializeServiceNodeName(builder, serviceNodeName);
        EcsJsonSerializer.serializeEventDataset(builder, eventDataset);
        EcsJsonSerializer.serializeThreadName(builder, record.getThreadName());
        EcsJsonSerializer.serializeLoggerName(builder, record.getLoggerName());
        EcsJsonSerializer.serializeAdditionalFields(builder, additionalFields);
        EcsJsonSerializer.serializeMDC(builder, record.getMdcCopy());
        String ndc = record.getNdc();
        if (ndc != null && !ndc.isEmpty()) {
            EcsJsonSerializer.serializeTagStart(builder);
            for (String tag : ndc.split("\\.")) {
                EcsJsonSerializer.serializeSingleTag(builder, tag);
            }
            EcsJsonSerializer.serializeTagEnd(builder);
        }
        if (includeOrigin && record.getSourceFileName() != null && record.getSourceMethodName() != null) {
            EcsJsonSerializer.serializeOrigin(builder, record.getSourceFileName(), record.getSourceMethodName(), record.getSourceLineNumber());
        }
        Throwable throwable = record.getThrown();
        if (throwable != null) {
            EcsJsonSerializer.serializeException(builder, throwable, stackTraceAsArray);
        }
        EcsJsonSerializer.serializeObjectEnd(builder);
        return builder.toString();
    }

    public void setIncludeOrigin(final boolean includeOrigin) {
        this.includeOrigin = includeOrigin;
    }

    public void setServiceName(final String serviceName) {
        this.serviceName = serviceName;
        eventDataset = EcsJsonSerializer.computeEventDataset(eventDataset, serviceName);
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

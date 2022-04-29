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

import co.elastic.logging.EcsJsonSerializer;
import co.elastic.logging.AdditionalField;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import java.util.ArrayList;
import java.util.List;

public class EcsLayout extends Layout {

    private static final MdcAccess MDC_ACCESS = MdcAccess.Resolver.resolve();

    private boolean stackTraceAsArray = false;
    private String serviceName;
    private String serviceVersion;

    private String serviceEnvironment;
    private String serviceNodeName;
    private boolean includeOrigin;
    private String eventDataset;
    private List<AdditionalField> additionalFields = new ArrayList<AdditionalField>();

    @Override
    public String format(LoggingEvent event) {
        StringBuilder builder = new StringBuilder();
        EcsJsonSerializer.serializeObjectStart(builder, event.timeStamp);
        EcsJsonSerializer.serializeLogLevel(builder, event.level.toString());
        EcsJsonSerializer.serializeFormattedMessage(builder, event.getRenderedMessage());
        EcsJsonSerializer.serializeEcsVersion(builder);
        EcsJsonSerializer.serializeServiceName(builder, serviceName);
        EcsJsonSerializer.serializeServiceVersion(builder, serviceVersion);
        EcsJsonSerializer.serializeServiceEnvironment(builder, serviceEnvironment);
        EcsJsonSerializer.serializeServiceNodeName(builder, serviceNodeName);
        EcsJsonSerializer.serializeEventDataset(builder, eventDataset);
        EcsJsonSerializer.serializeThreadName(builder, event.getThreadName());
        EcsJsonSerializer.serializeLoggerName(builder, event.categoryName);
        EcsJsonSerializer.serializeAdditionalFields(builder, additionalFields);
        EcsJsonSerializer.serializeMDC(builder, MDC_ACCESS.getMDC(event));
        EcsJsonSerializer.serializeTag(builder, event.getNDC());
        if (includeOrigin) {
            LocationInfo locationInformation = event.getLocationInformation();
            if (locationInformation != null) {
                EcsJsonSerializer.serializeOrigin(builder, locationInformation.getFileName(), locationInformation.getMethodName(), getLineNumber(locationInformation));
            }
        }
        ThrowableInformation throwableInformation = event.getThrowableInformation();
        if (throwableInformation != null) {
            EcsJsonSerializer.serializeException(builder, throwableInformation.getThrowable(), stackTraceAsArray);
        }
        EcsJsonSerializer.serializeObjectEnd(builder);
        return builder.toString();
    }

    private static int getLineNumber(LocationInfo locationInformation) {
        int lineNumber = -1;
        String lineNumberString = locationInformation.getLineNumber();
        if (!LocationInfo.NA.equals(lineNumberString)) {
            try {
                lineNumber = Integer.parseInt(lineNumberString);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return lineNumber;
    }

    @Override
    public boolean ignoresThrowable() {
        return false;
    }

    @Override
    public void activateOptions() {
        eventDataset = EcsJsonSerializer.computeEventDataset(eventDataset, serviceName);
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public void setServiceEnvironment(String serviceEnvironment) {
        this.serviceEnvironment = serviceEnvironment;
    }

    public void setServiceNodeName(String serviceNodeName) {
        this.serviceNodeName = serviceNodeName;
    }

    public void setIncludeOrigin(boolean includeOrigin) {
        this.includeOrigin = includeOrigin;
    }

    public void setStackTraceAsArray(boolean stackTraceAsArray) {
        this.stackTraceAsArray = stackTraceAsArray;
    }

    public void setEventDataset(String eventDataset) {
        this.eventDataset = eventDataset;
    }

    public void setAdditionalField(String additionalField) {
        this.additionalFields.add(AdditionalField.of(additionalField));
    }
}

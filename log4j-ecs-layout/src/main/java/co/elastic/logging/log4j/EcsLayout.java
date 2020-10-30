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
import org.apache.log4j.Layout;
import org.apache.log4j.MDC;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class EcsLayout extends Layout {

    private static final Method GET_PROPERTIES;

    static {
        Method getProperties = null;
        try {
            getProperties = LoggingEvent.class.getMethod("getProperties");
        } catch (NoSuchMethodException ignore) {
        }
        GET_PROPERTIES = getProperties;
    }

    private boolean stackTraceAsArray = false;
    private String serviceName;
    private boolean includeOrigin;
    private String eventDataset;

    @Override
    public String format(LoggingEvent event) {
        StringBuilder builder = new StringBuilder();
        EcsJsonSerializer.serializeObjectStart(builder, event.timeStamp);
        EcsJsonSerializer.serializeLogLevel(builder, event.level.toString());
        EcsJsonSerializer.serializeFormattedMessage(builder, event.getRenderedMessage());
        EcsJsonSerializer.serializeServiceName(builder, serviceName);
        EcsJsonSerializer.serializeEventDataset(builder, eventDataset);
        EcsJsonSerializer.serializeThreadName(builder, event.getThreadName());
        EcsJsonSerializer.serializeLoggerName(builder, event.categoryName);
        boolean syncAppender = Thread.currentThread().getName().equals(event.getThreadName());
        if (syncAppender) {
            // no need to create a copy of the mdc by calling event.getProperties()
            EcsJsonSerializer.serializeMDC(builder, MDC.getContext());
        } else if (GET_PROPERTIES == null) {
            // slow path if async appender and old log4j version
            try {
                EcsJsonSerializer.serializeMDC(builder, (Map) GET_PROPERTIES.invoke(event));
            } catch (IllegalAccessException ignore) {
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
            }
        }
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

    public void setIncludeOrigin(boolean includeOrigin) {
        this.includeOrigin = includeOrigin;
    }

    public void setStackTraceAsArray(boolean stackTraceAsArray) {
        this.stackTraceAsArray = stackTraceAsArray;
    }

    public void setEventDataset(String eventDataset) {
        this.eventDataset = eventDataset;
    }
}

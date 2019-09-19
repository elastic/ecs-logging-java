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
package co.elastic.logging.logback;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.encoder.EncoderBase;
import co.elastic.logging.EcsJsonSerializer;
import org.slf4j.Marker;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class EcsEncoder extends EncoderBase<ILoggingEvent> {

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private boolean stackTraceAsArray = false;
    private String serviceName;
    private boolean includeMarkers = false;
    private ThrowableProxyConverter throwableProxyConverter;
    private Set<String> topLevelLabels = new HashSet<String>(EcsJsonSerializer.DEFAULT_TOP_LEVEL_LABELS);
    private boolean includeOrigin;

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public void start() {
        super.start();
        throwableProxyConverter = new ThrowableProxyConverter();
        throwableProxyConverter.start();
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        StringBuilder builder = new StringBuilder();
        EcsJsonSerializer.serializeObjectStart(builder, event.getTimeStamp());
        EcsJsonSerializer.serializeLogLevel(builder, event.getLevel().toString());
        EcsJsonSerializer.serializeFormattedMessage(builder, event.getFormattedMessage());
        serializeMarkers(event, builder);
        EcsJsonSerializer.serializeServiceName(builder, serviceName);
        EcsJsonSerializer.serializeThreadName(builder, event.getThreadName());
        EcsJsonSerializer.serializeLoggerName(builder, event.getLoggerName());
        EcsJsonSerializer.serializeLabels(builder, event.getMDCPropertyMap(), topLevelLabels);
        if (includeOrigin) {
            StackTraceElement[] callerData = event.getCallerData();
            if (callerData != null && callerData.length > 0) {
                EcsJsonSerializer.serializeOrigin(builder, callerData[0]);
            }
        }
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy instanceof ThrowableProxy) {
            EcsJsonSerializer.serializeException(builder, ((ThrowableProxy) throwableProxy).getThrowable(), stackTraceAsArray);
        } else if (throwableProxy != null) {
            EcsJsonSerializer.serializeException(builder, throwableProxy.getClassName(), throwableProxy.getMessage(), throwableProxyConverter.convert(event), stackTraceAsArray);
        }
        EcsJsonSerializer.serializeObjectEnd(builder);
        // all these allocations kinda hurt
        return builder.toString().getBytes(UTF_8);
    }

    private void serializeMarkers(ILoggingEvent event, StringBuilder builder) {
        Marker marker = event.getMarker();
        if (includeMarkers && marker != null) {
            EcsJsonSerializer.serializeTagStart(builder);
            serializeMarker(builder, marker);
            EcsJsonSerializer.serializeTagEnd(builder);
        }
    }

    private void serializeMarker(StringBuilder builder, Marker marker) {
        if (marker != null) {
            EcsJsonSerializer.serializeSingleTag(builder, marker.getName());
            Iterator<Marker> it = marker.iterator();
            while (it.hasNext()) {
                serializeMarker(builder, it.next());
            }
        }
    }

    @Override
    public byte[] footerBytes() {
        return null;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setIncludeMarkers(boolean includeMarkers) {
        this.includeMarkers = includeMarkers;
    }

    public void setStackTraceAsArray(boolean stackTraceAsArray) {
        this.stackTraceAsArray = stackTraceAsArray;
    }

    public void setIncludeOrigin(boolean includeOrigin) {
        this.includeOrigin = includeOrigin;
    }
}

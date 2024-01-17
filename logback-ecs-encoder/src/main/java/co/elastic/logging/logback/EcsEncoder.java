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
package co.elastic.logging.logback;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.EncoderBase;
import co.elastic.logging.AdditionalField;
import co.elastic.logging.EcsJsonSerializer;
import org.slf4j.Marker;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EcsEncoder extends EncoderBase<ILoggingEvent> {

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private boolean stackTraceAsArray = false;
    private String serviceName;
    private String serviceVersion;
    private String serviceEnvironment;
    private String serviceNodeName;
    private String eventDataset;
    private boolean includeMarkers = false;
    private ThrowableHandlingConverter throwableConverter = null;
    private final ThrowableProxyConverter throwableProxyConverter = new ThrowableProxyConverter();
    private boolean includeOrigin;
    private final List<AdditionalField> additionalFields = new ArrayList<AdditionalField>();
    private OutputStream os;
    protected Layout<ILoggingEvent> messageLayout;

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public void start() {
        super.start();
        throwableProxyConverter.start();
        if (throwableConverter != null) {
            throwableConverter.start();
        }
        eventDataset = EcsJsonSerializer.computeEventDataset(eventDataset, serviceName);
    }

    /**
     * This method has been removed in logback 1.2.
     * To make this lib backwards compatible with logback 1.1 we have implement this method.
     */
    public void init(OutputStream os) {
        this.os = os;
    }

    /**
     * This method has been removed in logback 1.2.
     * To make this lib backwards compatible with logback 1.1 we have implement this method.
     * However, since we compile with 1.2.x, this method is not compiled as an interface method, which means that there won't be type
     * erasure. Therefore, we must use a {@link Object} argument for it to be compatible with 1.1.x.
     */
    public void doEncode(Object event) throws IOException {
        os.write(encode((ILoggingEvent) event));
        // on logback 1.1.x versions this encoder always works as if immediateFlush == true. In later versions, flushing is only handled by
        // the appender rather than the encoder
        os.flush();
    }

    /**
     * This method has been removed in logback 1.2.
     * To make this lib backwards compatible with logback 1.1 we have implement this method.
     */
    public void close() throws IOException {
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        StringBuilder builder = new StringBuilder(256);
        EcsJsonSerializer.serializeObjectStart(builder, event.getTimeStamp());
        EcsJsonSerializer.serializeLogLevel(builder, event.getLevel().toString());
        serializeMessage(event, builder);
        EcsJsonSerializer.serializeEcsVersion(builder);
        serializeMarkers(event, builder);
        EcsJsonSerializer.serializeServiceName(builder, serviceName);
        EcsJsonSerializer.serializeServiceVersion(builder, serviceVersion);
        EcsJsonSerializer.serializeServiceEnvironment(builder, serviceEnvironment);
        EcsJsonSerializer.serializeServiceNodeName(builder, serviceNodeName);
        EcsJsonSerializer.serializeEventDataset(builder, eventDataset);
        EcsJsonSerializer.serializeThreadName(builder, event.getThreadName());
        EcsJsonSerializer.serializeLoggerName(builder, event.getLoggerName());
        EcsJsonSerializer.serializeAdditionalFields(builder, additionalFields);
        EcsJsonSerializer.serializeMDC(builder, event.getMDCPropertyMap());
        if (includeOrigin) {
            StackTraceElement[] callerData = event.getCallerData();
            if (callerData != null && callerData.length > 0) {
                EcsJsonSerializer.serializeOrigin(builder, callerData[0]);
            }
        }
        // Allow subclasses to add custom fields. Calling this before the throwable serialization so we don't need to check for presence/absence of an ending comma.
        addCustomFields(event, builder);
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            if (throwableConverter != null) {
                EcsJsonSerializer.serializeException(builder, throwableProxy.getClassName(), throwableProxy.getMessage(), throwableConverter.convert(event), stackTraceAsArray);
            } else if (throwableProxy instanceof ThrowableProxy) {
                EcsJsonSerializer.serializeException(builder, ((ThrowableProxy) throwableProxy).getThrowable(), stackTraceAsArray);
            } else {
                EcsJsonSerializer.serializeException(builder, throwableProxy.getClassName(), throwableProxy.getMessage(), throwableProxyConverter.convert(event), stackTraceAsArray);
            }
        }
        EcsJsonSerializer.serializeObjectEnd(builder);
        // all these allocations kinda hurt
        return builder.toString().getBytes(UTF_8);
    }

    private void serializeMessage(ILoggingEvent event, StringBuilder builder) {
        if (messageLayout == null) {
            EcsJsonSerializer.serializeFormattedMessage(builder, event.getFormattedMessage());
        } else {
            EcsJsonSerializer.serializeFormattedMessage(builder, messageLayout.doLayout(event));
        }
    }

    /**
     * Subclasses can override this to add custom fields.
     * The last character in the StringBuilder will be comma when this is called.
     * You must add a comma after each custom field.
     */
    protected void addCustomFields(ILoggingEvent event, StringBuilder builder) {
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

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public void setServiceEnvironment(String serviceEnvironment) {
        this.serviceEnvironment = serviceEnvironment;
    }

    public void setServiceNodeName(String serviceNodeName) {
        this.serviceNodeName = serviceNodeName;
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

    public void addAdditionalField(AdditionalField pair) {
        this.additionalFields.add(pair);
    }

    public void setEventDataset(String eventDataset) {
        this.eventDataset = eventDataset;
    }

    public void setThrowableConverter(ThrowableHandlingConverter throwableConverter) {
        this.throwableConverter = throwableConverter;
    }

    /**
     * The supplied Layout will be applied specifically to format the <code>message</code> field based on the logging event.
     *
     * @param messageLayout
     */
    public void setMessageLayout(Layout<ILoggingEvent> messageLayout) {
        this.messageLayout = messageLayout;
    }
}

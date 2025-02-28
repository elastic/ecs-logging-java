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
package co.elastic.logging.log4j2;


import co.elastic.logging.EcsJsonSerializer;
import co.elastic.logging.JsonUtils;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.layout.Encoder;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MultiformatMessage;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.StringBuilderFormattable;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Plugin(name = "EcsLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE)
public class EcsLayout extends AbstractStringLayout {

    public static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final ObjectMessageJacksonSerializer JACKSON_SERIALIZER = ObjectMessageJacksonSerializer.Resolver.resolve();
    private static final MultiFormatHandler MULTI_FORMAT_HANDLER = MultiFormatHandler.Resolver.resolve();
    private static final boolean FORMAT_MESSAGES_PATTERN_DISABLE_LOOKUPS = PropertiesUtil.getProperties().getBooleanProperty(
            "log4j2.formatMsgNoLookups", false);

    private final KeyValuePair[] additionalFields;
    private final PatternFormatter[][] fieldValuePatternFormatter;
    private final boolean stackTraceAsArray;
    private final String serviceName;
    private final String serviceVersion;
    private final String serviceEnvironment;
    private final String serviceNodeName;
    private final String eventDataset;
    private final boolean includeMarkers;
    private final boolean includeOrigin;
    private final PatternFormatter[] exceptionPatternFormatter;
    private final ConcurrentMap<Class<? extends MultiformatMessage>, Boolean> supportsJson = new ConcurrentHashMap<Class<? extends MultiformatMessage>, Boolean>();
    private final MdcSerializer mdcSerializer;

    private EcsLayout(Configuration config, String serviceName, String serviceVersion, String serviceEnvironment, String serviceNodeName, String eventDataset, boolean includeMarkers,
                      KeyValuePair[] additionalFields, boolean includeOrigin, String exceptionPattern, boolean stackTraceAsArray, String mdcSerializerFullClassName) {
        super(config, UTF_8, null, null);
        this.serviceName = serviceName;
        this.serviceVersion = serviceVersion;
        this.serviceEnvironment = serviceEnvironment;
        this.serviceNodeName = serviceNodeName;
        this.eventDataset = eventDataset;
        this.includeMarkers = includeMarkers;
        this.includeOrigin = includeOrigin;
        this.stackTraceAsArray = stackTraceAsArray;
        this.additionalFields = additionalFields;
        fieldValuePatternFormatter = new PatternFormatter[additionalFields.length][];
        for (int i = 0; i < additionalFields.length; i++) {
            KeyValuePair additionalField = additionalFields[i];
            if (additionalField.getValue().contains("%")) {
                fieldValuePatternFormatter[i] = PatternLayout.createPatternParser(config)
                        .parse(additionalField.getValue())
                        .toArray(new PatternFormatter[0]);
            }
        }

        if (exceptionPattern != null && !exceptionPattern.isEmpty()) {
            exceptionPatternFormatter = PatternLayout.createPatternParser(config)
                    .parse(exceptionPattern)
                    .toArray(new PatternFormatter[0]);
        } else {
            exceptionPatternFormatter = null;
        }
        mdcSerializer = MdcSerializerResolver.resolve(mdcSerializerFullClassName);
    }

    @PluginBuilderFactory
    public static EcsLayout.Builder newBuilder() {
        return new EcsLayout.Builder();
    }

    private static boolean valueNeedsLookup(final String value) {
        return value != null && value.contains("${");
    }

    @Override
    public String toSerializable(LogEvent event) {
        final StringBuilder text = toText(event, getStringBuilder(), false);
        return text.toString();
    }

    @Override
    public void encode(LogEvent event, ByteBufferDestination destination) {
        final StringBuilder text = toText(event, getStringBuilder(), true);
        final Encoder<StringBuilder> helper = getStringBuilderEncoder();
        helper.encode(text, destination);
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    private StringBuilder toText(LogEvent event, StringBuilder builder, boolean gcFree) {
        EcsJsonSerializer.serializeObjectStart(builder, event.getTimeMillis());
        EcsJsonSerializer.serializeLogLevel(builder, event.getLevel().toString());
        serializeMessage(builder, gcFree, event.getMessage(), event.getThrown());
        EcsJsonSerializer.serializeEcsVersion(builder);
        EcsJsonSerializer.serializeServiceName(builder, serviceName);
        EcsJsonSerializer.serializeServiceVersion(builder, serviceVersion);
        EcsJsonSerializer.serializeServiceEnvironment(builder, serviceEnvironment);
        EcsJsonSerializer.serializeServiceNodeName(builder, serviceNodeName);
        EcsJsonSerializer.serializeEventDataset(builder, eventDataset);
        EcsJsonSerializer.serializeThreadName(builder, event.getThreadName());
        EcsJsonSerializer.serializeLoggerName(builder, event.getLoggerName());
        serializeAdditionalFieldsAndMDC(event, builder);
        serializeTags(event, builder);
        if (includeOrigin) {
            EcsJsonSerializer.serializeOrigin(builder, event.getSource());
        }
        serializeException(builder, event);
        EcsJsonSerializer.serializeObjectEnd(builder);
        return builder;
    }

    private void serializeAdditionalFieldsAndMDC(LogEvent event, StringBuilder builder) {
        final int length = additionalFields.length;
        if (length > 0) {
            final StrSubstitutor strSubstitutor = getConfiguration().getStrSubstitutor();
            for (int i = 0; i < length; i++) {
                KeyValuePair additionalField = additionalFields[i];
                PatternFormatter[] formatters = fieldValuePatternFormatter[i];
                CharSequence value = null;
                if (formatters != null) {
                    StringBuilder buffer = EcsJsonSerializer.getMessageStringBuilder();
                    formatPattern(event, formatters, buffer);
                    if (buffer.length() > 0) {
                        value = buffer;
                    }
                } else if (valueNeedsLookup(additionalField.getValue()) && !FORMAT_MESSAGES_PATTERN_DISABLE_LOOKUPS) {
                    StringBuilder lookupValue = EcsJsonSerializer.getMessageStringBuilder();
                    lookupValue.append(additionalField.getValue());
                    if (strSubstitutor.replaceIn(event, lookupValue)) {
                        value = lookupValue;
                    }
                } else {
                    value = additionalField.getValue();
                }

                if (value != null) {
                    builder.append('\"');
                    JsonUtils.quoteAsString(additionalField.getKey(), builder);
                    builder.append("\":\"");
                    JsonUtils.quoteAsString(EcsJsonSerializer.toNullSafeString(value), builder);
                    builder.append("\",");
                }
            }
        }
        mdcSerializer.serializeMdc(event, builder);
    }

    private static void formatPattern(LogEvent event, PatternFormatter[] formatters, StringBuilder buffer) {
        final int len = formatters.length;
        for (int i = 0; i < len; i++) {
            formatters[i].format(event, buffer);
        }
    }

    private void serializeTags(LogEvent event, StringBuilder builder) {
        ThreadContext.ContextStack stack = event.getContextStack();
        List<String> contextStack;
        if (stack == null) {
            contextStack = Collections.emptyList();
        } else {
            contextStack = stack.asList();
        }
        Marker marker = event.getMarker();
        boolean hasTags = !contextStack.isEmpty() || (includeMarkers && marker != null);
        if (hasTags) {
            EcsJsonSerializer.serializeTagStart(builder);
        }

        if (!contextStack.isEmpty()) {
            final int len = contextStack.size();
            for (int i = 0; i < len; i++) {
                builder.append('\"');
                JsonUtils.quoteAsString(contextStack.get(i), builder);
                builder.append("\",");
            }
        }

        if (includeMarkers && marker != null) {
            serializeMarker(builder, marker);
        }

        if (hasTags) {
            EcsJsonSerializer.serializeTagEnd(builder);
        }
    }

    private void serializeMarker(StringBuilder builder, Marker marker) {
        EcsJsonSerializer.serializeSingleTag(builder, marker.getName());
        if (marker.hasParents()) {
            Marker[] parents = marker.getParents();
            for (int i = 0; i < parents.length; i++) {
                serializeMarker(builder, parents[i]);
            }
        }
    }

    private void serializeMessage(StringBuilder builder, boolean gcFree, Message message, Throwable thrown) {
        if (message instanceof MultiformatMessage) {
            MultiformatMessage multiformatMessage = (MultiformatMessage) message;
            if (supportsJson(multiformatMessage)) {
                serializeJsonMessage(builder, multiformatMessage);
            } else {
                serializeSimpleMessage(builder, gcFree, message, thrown);
            }
        } else if (JACKSON_SERIALIZER != null && message instanceof ObjectMessage) {
            final StringBuilder jsonBuffer = EcsJsonSerializer.getMessageStringBuilder();
            JACKSON_SERIALIZER.formatTo(jsonBuffer, (ObjectMessage) message);
            addJson(builder, jsonBuffer);
        } else {
            serializeSimpleMessage(builder, gcFree, message, thrown);
        }
    }

    private static void serializeJsonMessage(StringBuilder builder, MultiformatMessage message) {
        final StringBuilder messageBuffer = EcsJsonSerializer.getMessageStringBuilder();
        MULTI_FORMAT_HANDLER.formatJsonTo(message, messageBuffer);
        addJson(builder, messageBuffer);
    }

    private static void addJson(StringBuilder buffer, StringBuilder jsonBuffer) {
        if (isObject(jsonBuffer)) {
            moveToRoot(jsonBuffer);
            buffer.append(jsonBuffer);
            buffer.append(", ");
        } else {
            buffer.append("\"message\":");
            if (isString(jsonBuffer)) {
                buffer.append(jsonBuffer);
            } else {
                // message always has to be a string to avoid mapping conflicts
                buffer.append('"');
                JsonUtils.quoteAsString(jsonBuffer, buffer);
                buffer.append('"');
            }
            buffer.append(", ");
        }
    }

    private void serializeSimpleMessage(StringBuilder builder, boolean gcFree, Message message, Throwable thrown) {
        builder.append("\"message\":\"");
        if (message instanceof CharSequence) {
            JsonUtils.quoteAsString(((CharSequence) message), builder);
        } else if (gcFree && message instanceof StringBuilderFormattable) {
            final StringBuilder messageBuffer = EcsJsonSerializer.getMessageStringBuilder();
            try {
                ((StringBuilderFormattable) message).formatTo(messageBuffer);
                JsonUtils.quoteAsString(messageBuffer, builder);
            } finally {
                trimToMaxSizeCopy(messageBuffer);
            }
        } else {
            JsonUtils.quoteAsString(EcsJsonSerializer.toNullSafeString(message.getFormattedMessage()), builder);
        }
        builder.append("\", ");
    }

    static void trimToMaxSizeCopy(final StringBuilder stringBuilder) {
        if (stringBuilder.length() > MAX_STRING_BUILDER_SIZE) {
            stringBuilder.setLength(MAX_STRING_BUILDER_SIZE);
            stringBuilder.trimToSize();
        }
    }

    private static boolean isObject(StringBuilder messageBuffer) {
        return messageBuffer.length() > 1 && messageBuffer.charAt(0) == '{' && messageBuffer.charAt(messageBuffer.length() - 1) == '}';
    }

    private static boolean isString(StringBuilder messageBuffer) {
        return messageBuffer.length() > 1 && messageBuffer.charAt(0) == '"' && messageBuffer.charAt(messageBuffer.length() - 1) == '"';
    }

    private static void moveToRoot(StringBuilder messageBuffer) {
        messageBuffer.setCharAt(0, ' ');
        messageBuffer.setCharAt(messageBuffer.length() -1, ' ');
    }

    private boolean supportsJson(MultiformatMessage message) {
        Boolean supportsJson = this.supportsJson.get(message.getClass());
        if (supportsJson == null) {
            supportsJson = false;
            for (String format : message.getFormats()) {
                if (format.equalsIgnoreCase("JSON")) {
                    supportsJson = true;
                    break;
                }
            }
            this.supportsJson.put(message.getClass(), supportsJson);
        }
        return supportsJson;
    }

    private void serializeException(StringBuilder messageBuffer, LogEvent event) {
        Throwable thrown = event.getThrown();
        if (thrown != null) {
            if (exceptionPatternFormatter != null) {
                StringBuilder stackTrace = EcsJsonSerializer.getMessageStringBuilder();
                formatPattern(event, exceptionPatternFormatter, stackTrace);
                EcsJsonSerializer.serializeException(messageBuffer, thrown.getClass().getName(), thrown.getMessage(), stackTrace, stackTraceAsArray);
            } else {
                EcsJsonSerializer.serializeException(messageBuffer, thrown, stackTraceAsArray);
            }
        }
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<EcsLayout> {

        @PluginConfiguration
        private Configuration configuration;
        @PluginBuilderAttribute("serviceName")
        private String serviceName;
        @PluginBuilderAttribute("serviceVersion")
        private String serviceVersion;
        @PluginBuilderAttribute("serviceEnvironment")
        private String serviceEnvironment;
        @PluginBuilderAttribute("serviceNodeName")
        private String serviceNodeName;
        @PluginBuilderAttribute("eventDataset")
        private String eventDataset;
        @PluginBuilderAttribute("includeMarkers")
        private boolean includeMarkers = false;
        @PluginBuilderAttribute("exceptionPattern")
        private String exceptionPattern;
        @PluginBuilderAttribute("stackTraceAsArray")
        private boolean stackTraceAsArray = false;
        @PluginElement("AdditionalField")
        private KeyValuePair[] additionalFields = new KeyValuePair[]{};
        @PluginBuilderAttribute("includeOrigin")
        private boolean includeOrigin = false;
        @PluginBuilderAttribute("mdcSerializer")
        private String mdcSerializerFullClassName = "";

        Builder() {
        }

        public Configuration getConfiguration() {
            return configuration;
        }

        public EcsLayout.Builder setConfiguration(final Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public KeyValuePair[] getAdditionalFields() {
            return additionalFields.clone();
        }

        public String getServiceName() {
            return serviceName;
        }

        public String getServiceVersion() {
            return serviceVersion;
        }

        public String getServiceEnvironment() { return serviceEnvironment; }

        public String getServiceNodeName() {
            return serviceNodeName;
        }

        public String getEventDataset() {
            return eventDataset;
        }

        public boolean isIncludeMarkers() {
            return includeMarkers;
        }

        public boolean isIncludeOrigin() {
            return includeOrigin;
        }

        public boolean isStackTraceAsArray() {
            return stackTraceAsArray;
        }

        public String getExceptionPattern() {
            return exceptionPattern;
        }

        public String getMdcSerializerFullClassName() {
            return mdcSerializerFullClassName;
        }

        /**
         * Additional fields to set on each log event.
         *
         * @return this builder
         */
        public EcsLayout.Builder setAdditionalFields(final KeyValuePair[] additionalFields) {
            this.additionalFields = additionalFields.clone();
            return this;
        }

        public EcsLayout.Builder setServiceName(final String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public EcsLayout.Builder setServiceVersion(final String serviceVersion) {
            this.serviceVersion = serviceVersion;
            return this;
        }

        public EcsLayout.Builder setServiceEnvironment(final String serviceEnvironment) {
            this.serviceEnvironment = serviceEnvironment;
            return this;
        }

        public EcsLayout.Builder setServiceNodeName(final String serviceNodeName) {
            this.serviceNodeName = serviceNodeName;
            return this;
        }

        public EcsLayout.Builder setEventDataset(String eventDataset) {
            this.eventDataset = eventDataset;
            return this;
        }

        public EcsLayout.Builder setIncludeMarkers(final boolean includeMarkers) {
            this.includeMarkers = includeMarkers;
            return this;
        }

        public EcsLayout.Builder setIncludeOrigin(final boolean includeOrigin) {
            this.includeOrigin = includeOrigin;
            return this;
        }

        public EcsLayout.Builder setStackTraceAsArray(boolean stackTraceAsArray) {
            this.stackTraceAsArray = stackTraceAsArray;
            return this;
        }

        public EcsLayout.Builder setExceptionPattern(String exceptionPattern) {
            this.exceptionPattern = exceptionPattern;
            return this;
        }

        public EcsLayout.Builder setMdcSerializerFullClassName(String mdcSerializerFullClassName) {
            this.mdcSerializerFullClassName = mdcSerializerFullClassName;
            return this;
        }

        @Override
        public EcsLayout build() {
            return new EcsLayout(getConfiguration(), serviceName, serviceVersion, serviceEnvironment, serviceNodeName,
                    EcsJsonSerializer.computeEventDataset(eventDataset, serviceName),
                    includeMarkers, additionalFields, includeOrigin, exceptionPattern, stackTraceAsArray, mdcSerializerFullClassName);
        }
    }
}


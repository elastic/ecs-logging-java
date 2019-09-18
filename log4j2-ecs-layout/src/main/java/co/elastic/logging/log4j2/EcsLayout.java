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
package co.elastic.logging.log4j2;


import co.elastic.logging.EcsJsonSerializer;
import co.elastic.logging.JsonUtils;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
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
import org.apache.logging.log4j.util.MultiFormatStringBuilderFormattable;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.apache.logging.log4j.util.TriConsumer;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Plugin(name = "EcsLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE)
public class EcsLayout extends AbstractStringLayout {

    public static final Charset UTF_8 = Charset.forName("UTF-8");
    public static final String[] JSON_FORMAT = {"JSON"};

    private final TriConsumer<String, Object, StringBuilder> WRITE_KEY_VALUES_INTO = new TriConsumer<String, Object, StringBuilder>() {
        @Override
        public void accept(final String key, final Object value, final StringBuilder stringBuilder) {
            stringBuilder.append('\"');
            if (!topLevelLabels.contains(key)) {
                stringBuilder.append("labels.");
            }
            JsonUtils.quoteAsString(key, stringBuilder);
            stringBuilder.append("\":\"");
            JsonUtils.quoteAsString(EcsJsonSerializer.toNullSafeString(String.valueOf(value)), stringBuilder);
            stringBuilder.append("\",");
        }
    };

    private final KeyValuePair[] additionalFields;
    private final PatternFormatter[][] fieldValuePatternFormatter;
    private final Set<String> topLevelLabels;
    private final boolean stackTraceAsArray;
    private final String serviceName;
    private final boolean includeMarkers;
    private final boolean includeOrigin;
    private final ConcurrentMap<Class<? extends MultiformatMessage>, Boolean> supportsJson = new ConcurrentHashMap<Class<? extends MultiformatMessage>, Boolean>();

    private EcsLayout(Configuration config, String serviceName, boolean includeMarkers, KeyValuePair[] additionalFields, Collection<String> topLevelLabels, boolean includeOrigin, boolean stackTraceAsArray) {
        super(config, UTF_8, null, null);
        this.serviceName = serviceName;
        this.includeMarkers = includeMarkers;
        this.topLevelLabels = new HashSet<String>(topLevelLabels);
        this.includeOrigin = includeOrigin;
        this.stackTraceAsArray = stackTraceAsArray;
        this.topLevelLabels.add("trace.id");
        this.topLevelLabels.add("transaction.id");
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
    }

    @PluginBuilderFactory
    public static EcsLayout.Builder newBuilder() {
        return new EcsLayout.Builder().asBuilder();
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

    private StringBuilder toText(LogEvent event, StringBuilder builder, boolean gcFree) {
        EcsJsonSerializer.serializeObjectStart(builder, event.getTimeMillis());
        EcsJsonSerializer.serializeLogLevel(builder, event.getLevel().toString());
        serializeMessage(builder, gcFree, event.getMessage(), event.getThrown());
        EcsJsonSerializer.serializeServiceName(builder, serviceName);
        EcsJsonSerializer.serializeThreadName(builder, event.getThreadName());
        EcsJsonSerializer.serializeLoggerName(builder, event.getLoggerName());
        serializeLabels(event, builder);
        serializeTags(event, builder);
        if (includeOrigin) {
            EcsJsonSerializer.serializeOrigin(builder, event.getSource());
        }
        EcsJsonSerializer.serializeException(builder, event.getThrown(), stackTraceAsArray);
        EcsJsonSerializer.serializeObjectEnd(builder);
        return builder;
    }

    private void serializeLabels(LogEvent event, StringBuilder builder) {
        final int length = additionalFields.length;
        if (!event.getContextData().isEmpty() || length > 0) {
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
                    } else if (valueNeedsLookup(additionalField.getValue())) {
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
            event.getContextData().forEach(WRITE_KEY_VALUES_INTO, builder);
        }
    }

    private static void formatPattern(LogEvent event, PatternFormatter[] formatters, StringBuilder buffer) {
        final int len = formatters.length;
        for (int i = 0; i < len; i++) {
            formatters[i].format(event, buffer);
        }
    }

    private void serializeTags(LogEvent event, StringBuilder builder) {
        List<String> contextStack = event.getContextStack().asList();
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
        } else {
            serializeSimpleMessage(builder, gcFree, message, thrown);
        }
    }

    private void serializeJsonMessage(StringBuilder builder, MultiformatMessage message) {
        final StringBuilder messageBuffer = EcsJsonSerializer.getMessageStringBuilder();
        if (message instanceof MultiFormatStringBuilderFormattable) {
            ((MultiFormatStringBuilderFormattable) message).formatTo(JSON_FORMAT, messageBuffer);
        } else {
            messageBuffer.append(message.getFormattedMessage(JSON_FORMAT));
        }
        if (isObject(messageBuffer)) {
            moveToRoot(messageBuffer);
            builder.append(messageBuffer);
            builder.append(", ");
        } else {
            builder.append("\"message\":");
            builder.append(messageBuffer);
            builder.append(", ");
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
                trimToMaxSize(messageBuffer);
            }
        } else {
            JsonUtils.quoteAsString(EcsJsonSerializer.toNullSafeString(message.getFormattedMessage()), builder);
        }
        builder.append("\", ");
    }

    private boolean isObject(StringBuilder messageBuffer) {
        return messageBuffer.length() > 1 && messageBuffer.charAt(0) == '{' && messageBuffer.charAt(messageBuffer.length() -1) == '}';
    }

    private void moveToRoot(StringBuilder messageBuffer) {
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

    public static class Builder extends AbstractStringLayout.Builder<EcsLayout.Builder>
            implements org.apache.logging.log4j.core.util.Builder<EcsLayout> {

        @PluginBuilderAttribute("serviceName")
        private String serviceName;
        @PluginBuilderAttribute("includeMarkers")
        private boolean includeMarkers = false;
        @PluginBuilderAttribute("stackTraceAsArray")
        private boolean stackTraceAsArray = false;
        @PluginElement("AdditionalField")
        private KeyValuePair[] additionalFields;
        @PluginElement("TopLevelLabels")
        private String[] topLevelLabels;
        @PluginBuilderAttribute("includeOrigin")
        private boolean includeOrigin;

        Builder() {
            super();
            setCharset(UTF_8);
        }

        public KeyValuePair[] getAdditionalFields() {
            return additionalFields;
        }

        public String getServiceName() {
            return serviceName;
        }

        public boolean isIncludeMarkers() {
            return includeMarkers;
        }

        public boolean isIncludeOrigin() {
            return includeOrigin;
        }

        public String[] getTopLevelLabels() {
            return topLevelLabels;
        }

        public EcsLayout.Builder setTopLevelLabels(final String[] topLevelLabels) {
            this.topLevelLabels = topLevelLabels;
            return asBuilder();
        }

        /**
         * Additional fields to set on each log event.
         *
         * @return this builder
         */
        public EcsLayout.Builder setAdditionalFields(final KeyValuePair[] additionalFields) {
            this.additionalFields = additionalFields;
            return asBuilder();
        }

        public EcsLayout.Builder setServiceName(final String serviceName) {
            this.serviceName = serviceName;
            return asBuilder();
        }

        public EcsLayout.Builder setIncludeMarkers(final boolean includeMarkers) {
            this.includeMarkers = includeMarkers;
            return asBuilder();
        }

        public EcsLayout.Builder setIncludeOrigin(final boolean includeOrigin) {
            this.includeOrigin = includeOrigin;
            return asBuilder();
        }

        public EcsLayout.Builder setStackTraceAsArray(boolean stackTraceAsArray) {
            this.stackTraceAsArray = stackTraceAsArray;
            return asBuilder();
        }

        @Override
        public EcsLayout build() {
            return new EcsLayout(getConfiguration(), serviceName, includeMarkers, additionalFields, topLevelLabels == null ? Collections.<String>emptyList() : Arrays.<String>asList(topLevelLabels), includeOrigin, stackTraceAsArray);
        }

        public boolean isStackTraceAsArray() {
            return stackTraceAsArray;
        }
    }
}


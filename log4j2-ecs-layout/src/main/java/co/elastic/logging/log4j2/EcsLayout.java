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
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.JsonUtils;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.core.util.StringBuilderWriter;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.apache.logging.log4j.util.TriConsumer;

import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Plugin(name = "EcsLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE)
public class EcsLayout extends AbstractStringLayout {

    private static final ThreadLocal<StringBuilder> messageStringBuilder = new ThreadLocal<StringBuilder>();

    private final TriConsumer<String, Object, StringBuilder> WRITE_KEY_VALUES_INTO = new TriConsumer<String, Object, StringBuilder>() {
        @Override
        public void accept(final String key, final Object value, final StringBuilder stringBuilder) {
            stringBuilder.append('\"');
            if (!topLevelLabels.contains(key)) {
                stringBuilder.append("labels.");
            }
            co.elastic.logging.JsonUtils.quoteAsString(key, stringBuilder);
            stringBuilder.append("\":\"");
            co.elastic.logging.JsonUtils.quoteAsString(EcsJsonSerializer.toNullSafeString(String.valueOf(value)), stringBuilder);
            stringBuilder.append("\",");
        }
    };

    private final KeyValuePair[] additionalFields;
    private final Set<String> topLevelLabels;
    private String serviceName;

    private EcsLayout(Configuration config, String serviceName, KeyValuePair[] additionalFields, Collection<String> topLevelLabels) {
        super(config, Charset.forName("UTF-8"), null, null);
        this.serviceName = serviceName;
        this.topLevelLabels = new HashSet<String>(topLevelLabels);
        this.topLevelLabels.add("trace.id");
        this.topLevelLabels.add("transaction.id");
        this.additionalFields = additionalFields;
    }

    @PluginBuilderFactory
    public static EcsLayout.Builder newBuilder() {
        return new EcsLayout.Builder().asBuilder();
    }

    private static StringBuilder getMessageStringBuilder() {
        StringBuilder result = messageStringBuilder.get();
        if (result == null) {
            result = new StringBuilder(DEFAULT_STRING_BUILDER_SIZE);
            messageStringBuilder.set(result);
        }
        result.setLength(0);
        return result;
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
        EcsJsonSerializer.serializeObjectEnd(builder);
        return builder;
    }

    private void serializeLabels(LogEvent event, StringBuilder builder) {
        if (!event.getContextData().isEmpty() || additionalFields.length > 0) {
            if (additionalFields.length > 0) {
                final StrSubstitutor strSubstitutor = getConfiguration().getStrSubstitutor();
                for (KeyValuePair additionalField : additionalFields) {
                    builder.append('\"');
                    JsonUtils.quoteAsString(additionalField.getKey(), builder);
                    builder.append("\":\"");
                    final String value = valueNeedsLookup(additionalField.getValue())
                            ? strSubstitutor.replace(event, additionalField.getValue())
                            : additionalField.getValue();
                    JsonUtils.quoteAsString(EcsJsonSerializer.toNullSafeString(value), builder);
                    builder.append("\",");
                }
            }
            event.getContextData().forEach(WRITE_KEY_VALUES_INTO, builder);
        }
    }

    private void serializeTags(LogEvent event, StringBuilder builder) {
        List<String> contextStack = event.getContextStack().asList();
        if (!contextStack.isEmpty()) {
            builder.append("\"tags\":[");
            for (int i = 0; i < contextStack.size(); i++) {
                builder.append('\"');
                JsonUtils.quoteAsString(contextStack.get(i), builder);
                builder.append("\",");
            }
            // removes last comma
            builder.setLength(builder.length() - 1);
            builder.append("],");
        }
    }

    private void serializeMessage(StringBuilder builder, boolean gcFree, Message message, Throwable thrown) {
        builder.append("\"message\":\"");
        if (message instanceof CharSequence) {
            JsonUtils.quoteAsString(((CharSequence) message), builder);
        } else if (gcFree && message instanceof StringBuilderFormattable) {
            final StringBuilder messageBuffer = getMessageStringBuilder();
            try {
                ((StringBuilderFormattable) message).formatTo(messageBuffer);
                JsonUtils.quoteAsString(messageBuffer, builder);
            } finally {
                trimToMaxSize(messageBuffer);
            }
        } else {
            JsonUtils.quoteAsString(EcsJsonSerializer.toNullSafeString(message.getFormattedMessage()), builder);
        }
        if (thrown != null) {
            builder.append("\\n");
            JsonUtils.quoteAsString(formatThrowable(thrown), builder);
        }
        builder.append("\", ");
        if (message instanceof MapMessage) {
            MapMessage mapMessage = (MapMessage) message;
            mapMessage.forEach(WRITE_KEY_VALUES_INTO, builder);
        }
    }

    private static CharSequence formatThrowable(final Throwable throwable) {
        StringBuilderWriter sw = new StringBuilderWriter(getMessageStringBuilder());
        final PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.flush();
        return sw.getBuilder();
    }

    public static class Builder extends AbstractStringLayout.Builder<EcsLayout.Builder>
            implements org.apache.logging.log4j.core.util.Builder<EcsLayout> {

        @PluginBuilderAttribute("serviceName")
        private String serviceName;
        @PluginElement("AdditionalField")
        private KeyValuePair[] additionalFields;
        @PluginElement("TopLevelLabels")
        private String[] topLevelLabels;

        Builder() {
            super();
            setCharset(Charset.forName("UTF-8"));
        }

        public KeyValuePair[] getAdditionalFields() {
            return additionalFields;
        }

        public String getServiceName() {
            return serviceName;
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

        @Override
        public EcsLayout build() {
            return new EcsLayout(getConfiguration(), serviceName, additionalFields, topLevelLabels == null ? Collections.<String>emptyList() : Arrays.<String>asList(topLevelLabels));
        }
    }
}


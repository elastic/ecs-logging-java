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
package co.elastic.logging;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EcsJsonSerializer {

    private static final TimestampSerializer TIMESTAMP_SERIALIZER = new TimestampSerializer();
    private static final ThreadLocal<StringBuilder> messageStringBuilder = new ThreadLocal<StringBuilder>();
    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final Pattern NEW_LINE_PATTERN = Pattern.compile("\\r\\n|\\n|\\r");

    public static CharSequence toNullSafeString(final CharSequence s) {
        return s == null ? "" : s;
    }

    public static void serializeObjectStart(StringBuilder builder, long timeMillis) {
        builder.append('{');
        builder.append("\"@timestamp\":\"");
        TIMESTAMP_SERIALIZER.serializeEpochTimestampAsIsoDateTime(builder, timeMillis);
        builder.append("\",");
    }

    public static void serializeEcsVersion(StringBuilder builder) {
        builder.append("\"ecs.version\": \"1.2.0\",");
    }

    public static void serializeObjectEnd(StringBuilder builder) {
        removeIfEndsWith(builder, ",");
        builder.append('}');
        builder.append('\n');
    }

    public static void serializeLoggerName(StringBuilder builder, String loggerName) {
        if (loggerName != null) {
            builder.append("\"log.logger\":\"");
            JsonUtils.quoteAsString(loggerName, builder);
            builder.append("\",");
        }
    }

    public static void serializeThreadName(StringBuilder builder, String threadName) {
        if (threadName != null) {
            builder.append("\"process.thread.name\":\"");
            JsonUtils.quoteAsString(threadName, builder);
            builder.append("\",");
        }
    }

    public static void serializeThreadId(StringBuilder builder, long threadId) {
        builder.append("\"process.thread.id\":");
        builder.append(threadId);
        builder.append(",");
    }

    public static void serializeFormattedMessage(StringBuilder builder, String message) {
        builder.append("\"message\":\"");
        JsonUtils.quoteAsString(message, builder);
        builder.append("\",");
    }

    public static void serializeServiceName(StringBuilder builder, String serviceName) {
        if (serviceName != null) {
            builder.append("\"service.name\":\"");
            JsonUtils.quoteAsString(serviceName, builder);
            builder.append("\",");
        }
    }

    public static void serializeServiceVersion(StringBuilder builder, String serviceVersion) {
        if (serviceVersion != null) {
            builder.append("\"service.version\":\"");
            JsonUtils.quoteAsString(serviceVersion, builder);
            builder.append("\",");
        }
    }

    public static void serializeServiceEnvironment(StringBuilder builder, String serviceEnvironment) {
        if (serviceEnvironment != null) {
            builder.append("\"service.environment\":\"");
            JsonUtils.quoteAsString(serviceEnvironment, builder);
            builder.append("\",");
        }
    }

    public static void serializeServiceNodeName(StringBuilder builder, String serviceNodeName) {
        if (serviceNodeName != null) {
            builder.append("\"service.node.name\":\"");
            JsonUtils.quoteAsString(serviceNodeName, builder);
            builder.append("\",");
        }
    }

    public static void serializeEventDataset(StringBuilder builder, String eventDataset) {
        if (eventDataset != null) {
            builder.append("\"event.dataset\":\"");
            JsonUtils.quoteAsString(eventDataset, builder);
            builder.append("\",");
        }
    }

    public static void serializeLogLevel(StringBuilder builder, String level) {
        builder.append("\"log.level\":");
        // add padding so that all levels line up
        //  WARN
        // ERROR
        for (int i = 5 - level.length(); i > 0; i--) {
            builder.append(' ');
        }
        builder.append('\"');
        JsonUtils.quoteAsString(level, builder);
        builder.append("\",");
    }

    public static void serializeTag(StringBuilder builder, String tag) {
        if (tag != null) {
            builder.append("\"tags\":[\"");
            JsonUtils.quoteAsString(tag, builder);
            builder.append("\"],");
        }
    }

    public static void serializeTagStart(StringBuilder builder) {
        builder.append("\"tags\":[");
    }

    public static void serializeSingleTag(StringBuilder builder, String tag) {
        if (tag != null) {
            builder.append("\"");
            JsonUtils.quoteAsString(tag, builder);
            builder.append("\",");
        }
    }

    public static void serializeTagEnd(StringBuilder builder) {
        builder.setLength(builder.length() - 1);
        builder.append("],");
    }

    public static void serializeOrigin(StringBuilder builder, StackTraceElement stackTraceElement) {
        if (stackTraceElement != null) {
            serializeOrigin(builder, stackTraceElement.getFileName(), stackTraceElement.getMethodName(), stackTraceElement.getLineNumber());
        }
    }

    public static void serializeOrigin(StringBuilder builder, String fileName, String methodName, int lineNumber) {
        builder.append("\"log\":{");
        builder.append("\"origin\":{");
        builder.append("\"file\":{");
        builder.append("\"name\":\"");
        JsonUtils.quoteAsString(fileName, builder);
        builder.append('"');
        if (lineNumber >= 0) {
            builder.append(',');
            builder.append("\"line\":");
            builder.append(lineNumber);
        }
        builder.append("},");
        builder.append("\"function\":\"");
        JsonUtils.quoteAsString(methodName, builder);
        builder.append('"');
        builder.append("}");
        builder.append("},");
    }

    public static void serializeMDC(StringBuilder builder, Map<String, ?> properties) {
        if (properties != null && !properties.isEmpty()) {
            for (Map.Entry<String, ?> entry : properties.entrySet()) {
                builder.append('\"');
                String key = entry.getKey();
                JsonUtils.quoteAsString(key, builder);
                builder.append("\":\"");
                JsonUtils.quoteAsString(toNullSafeString(String.valueOf(entry.getValue())), builder);
                builder.append("\",");
            }
        }
    }

    public static void serializeException(StringBuilder builder, Throwable thrown, boolean stackTraceAsArray) {
        if (thrown != null) {
            builder.append("\"error.type\":\"");
            JsonUtils.quoteAsString(thrown.getClass().getName(), builder);
            builder.append('\"');

            String message = thrown.getMessage();
            if (message != null) {
                builder.append(",\"error.message\":\"");
                JsonUtils.quoteAsString(message, builder);
                builder.append('\"');
            }

            int prevLength = builder.length();
            builder.append(",\"error.stack_trace\":").append(stackTraceAsArray ? '[' : "\"");
            if (formatThrowable(builder, thrown, stackTraceAsArray)) {
                builder.append(stackTraceAsArray ? ']' : '\"');
            } else {
                builder.setLength(prevLength); // reset if no stacktrace was written
            }
        }
    }

    public static void serializeException(StringBuilder builder, String exceptionClassName, CharSequence exceptionMessage, CharSequence stackTrace, boolean stackTraceAsArray) {
        builder.append("\"error.type\":\"");
        JsonUtils.quoteAsString(exceptionClassName, builder);
        builder.append("\",");

        if (exceptionMessage != null) {
            builder.append("\"error.message\":\"");
            JsonUtils.quoteAsString(exceptionMessage, builder);
            builder.append("\",");
        }
        if (stackTraceAsArray) {
            builder.append("\"error.stack_trace\":[");
            formatStackTraceAsArray(builder, stackTrace);
            builder.append("]");
        } else {
            builder.append("\"error.stack_trace\":\"");
            JsonUtils.quoteAsString(stackTrace, builder);
            builder.append("\"");
        }
    }

    private static boolean formatThrowable(final StringBuilder jsonBuilder, final Throwable throwable, final boolean stackTraceAsArray) {
        final StringBuilder buffer = getMessageStringBuilder();
        final int initialLength = jsonBuilder.length();
        final PrintWriter pw = new PrintWriter(new StringBuilderWriter(buffer), true) {
            private int lines = 0;

            @Override
            public void println() {
                flush();
                if (stackTraceAsArray) {
                    if (lines > 0) jsonBuilder.append(',');
                    jsonBuilder.append(NEW_LINE).append("\t\"");
                    JsonUtils.quoteAsString(buffer, jsonBuilder);
                    jsonBuilder.append('\"');
                } else {
                    JsonUtils.quoteAsString(buffer, jsonBuilder);
                    JsonUtils.quoteAsString(NEW_LINE, jsonBuilder);
                }
                buffer.setLength(0);
                lines++;
            }

            @Override
            public void close() {
                if (lines <= 1) {
                    jsonBuilder.setLength(initialLength); // skip the first line (message) if no stacktrace follows
                }
            }
        };
        throwable.printStackTrace(pw);
        pw.close();
        return jsonBuilder.length() > initialLength;
    }

    private static void formatStackTraceAsArray(StringBuilder builder, CharSequence stackTrace) {
        builder.append(NEW_LINE);

        // splits the stackTrace by new lines
        Matcher matcher = NEW_LINE_PATTERN.matcher(stackTrace);
        if (matcher.find()) {
            int index = 0;
            do {
                int start = matcher.start();
                int end = matcher.end();
                if (index == 0 && index == start && start == end) {
                    // no empty leading substring included for zero-width match
                    // at the beginning of the input char sequence.
                    continue;
                }

                // append non-last line
                appendStackTraceLine(builder, stackTrace, index, start);
                builder.append(',');
                builder.append(NEW_LINE);
                index = end;
            } while (matcher.find());

            int length = stackTrace.length();
            if (index < length) {
                // append remaining line
                appendStackTraceLine(builder, stackTrace, index, length);
            }
        } else {
            // no newlines found, add entire stack trace as single element
            appendStackTraceLine(builder, stackTrace, 0, stackTrace.length());
        }
    }

    private static void appendStackTraceLine(StringBuilder builder, CharSequence stackTrace, int start, int end) {
        builder.append("\t\"");
        JsonUtils.quoteAsString(stackTrace, start, end, builder);
        builder.append("\"");
    }

    public static void removeIfEndsWith(StringBuilder sb, String ending) {
        if (endsWith(sb, ending)) {
            sb.setLength(sb.length() - ending.length());
        }
    }

    public static boolean endsWith(StringBuilder sb, String ending) {
        int endingLength = ending.length();
        int startIndex = sb.length() - endingLength;
        if (startIndex < 0) {
            return false;
        }
        for (int i = 0; i < endingLength; i++) {
            if (sb.charAt(startIndex + i) != ending.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public static StringBuilder getMessageStringBuilder() {
        StringBuilder result = messageStringBuilder.get();
        if (result == null) {
            result = new StringBuilder(1024);
            messageStringBuilder.set(result);
        }
        result.setLength(0);
        return result;
    }

    public static String computeEventDataset(String eventDataset, String serviceName) {
        if (eventDataset == null && serviceName != null && !serviceName.isEmpty()) {
            return serviceName;
        }
        return eventDataset;
    }

    public static void serializeAdditionalFields(StringBuilder builder, List<AdditionalField> additionalFields) {
        if (!additionalFields.isEmpty()) {
            for (int i = 0, size = additionalFields.size(); i < size; i++) {
                AdditionalField additionalField = additionalFields.get(i);
                if (additionalField.getKey() != null) {
                    builder.append('\"');
                    JsonUtils.quoteAsString(additionalField.getKey(), builder);
                    builder.append("\":\"");
                    JsonUtils.quoteAsString(additionalField.getValue(), builder);
                    builder.append("\",");
                }
            }
        }
    }

    private static class StringBuilderWriter extends Writer {

        private final StringBuilder buffer;

        StringBuilderWriter(StringBuilder buffer) {
            this.buffer = buffer;
        }

        @Override
        public Writer append(CharSequence csq) {
            buffer.append(csq);
            return this;
        }

        @Override
        public void write(String str) {
            buffer.append(str);
        }

        @Override
        public void write(String str, int off, int len) {
            buffer.append(str, off, len);
        }

        @Override
        public Writer append(CharSequence csq, int start, int end) {
            buffer.append(csq, start, end);
            return this;
        }

        @Override
        public Writer append(char c) {
            buffer.append(c);
            return this;
        }

        @Override
        public void write(int c) {
            buffer.append((char) c);
        }

        @Override
        public void write(char[] cbuf, int off, int len) {
            buffer.append(cbuf, off, len);
        }

        @Override
        public void flush() {

        }

        @Override
        public void close() {

        }
    }
}

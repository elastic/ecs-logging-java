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
package co.elastic.logging;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EcsJsonSerializer {

    public static final List<String> DEFAULT_TOP_LEVEL_LABELS = Arrays.asList("trace.id", "transaction.id", "span.id", "error.id", "service.name");
    private static final TimestampSerializer TIMESTAMP_SERIALIZER = new TimestampSerializer();
    private static final ThreadLocal<StringBuilder> messageStringBuilder = new ThreadLocal<StringBuilder>();
    private static final  String NEW_LINE = System.getProperty("line.separator");

    public static CharSequence toNullSafeString(final CharSequence s) {
        return s == null ? "" : s;
    }

    public static void serializeObjectStart(StringBuilder builder, long timeMillis) {
        builder.append('{');
        builder.append("\"@timestamp\":\"");
        TIMESTAMP_SERIALIZER.serializeEpochTimestampAsIsoDateTime(builder, timeMillis);
        builder.append("\", ");
    }

    public static void serializeObjectEnd(StringBuilder builder) {
        removeIfEndsWith(builder, ",");
        builder.append('}');
        builder.append('\n');
    }

    public static void serializeLoggerName(StringBuilder builder, String loggerName) {
        builder.append("\"log.logger\":\"");
        JsonUtils.quoteAsString(loggerName, builder);
        builder.append("\",");
    }

    public static void serializeThreadName(StringBuilder builder, String threadName) {
        if (threadName != null) {
            builder.append("\"process.thread.name\":\"");
            JsonUtils.quoteAsString(threadName, builder);
            builder.append("\",");
        }
    }

    public static void serializeFormattedMessage(StringBuilder builder, String message) {
        builder.append("\"message\":\"");
        JsonUtils.quoteAsString(message, builder);
        builder.append("\", ");
    }

    public static void serializeServiceName(StringBuilder builder, String serviceName) {
        if (serviceName != null) {
            builder.append("\"service.name\":\"").append(serviceName).append("\",");
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
        builder.append("\", ");
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
        builder.append("\"log.origin\":{");
        builder.append("\"file.name\":\"");
        JsonUtils.quoteAsString(fileName, builder);
        builder.append("\",");
        builder.append("\"function\":\"");
        JsonUtils.quoteAsString(methodName, builder);
        builder.append("\",");
        builder.append("\"file.line\":");
        builder.append(lineNumber);
        builder.append("},");
    }

    public static void serializeLabels(StringBuilder builder, Map<String, ?> labels, Set<String> topLevelLabels) {
        if (!labels.isEmpty()) {
            for (Map.Entry<String, ?> entry : labels.entrySet()) {
                builder.append('\"');
                String key = entry.getKey();
                if (!topLevelLabels.contains(key)) {
                    builder.append("labels.");
                }
                JsonUtils.quoteAsString(key, builder);
                builder.append("\":\"");
                JsonUtils.quoteAsString(toNullSafeString(String.valueOf(entry.getValue())), builder);
                builder.append("\",");
            }
        }
    }

    public static void serializeException(StringBuilder builder, Throwable thrown, boolean stackTraceAsArray) {
        if (thrown != null) {
            builder.append("\"error.code\":\"");
            JsonUtils.quoteAsString(thrown.getClass().getName(), builder);
            builder.append("\",");
            builder.append("\"error.message\":\"");
            JsonUtils.quoteAsString(thrown.getMessage(), builder);
            builder.append("\",");
            if (stackTraceAsArray) {
                builder.append("\"error.stack_trace\":[").append(NEW_LINE);
                formatThrowableAsArray(builder, thrown);
                builder.append("]");
            } else {
                builder.append("\"error.stack_trace\":\"");
                JsonUtils.quoteAsString(formatThrowable(thrown), builder);
                builder.append("\"");
            }
        }
    }

    public static void serializeException(StringBuilder builder, String exceptionClassName, String exceptionMessage, String stackTrace, boolean stackTraceAsArray) {
        builder.append("\"error.code\":\"");
        JsonUtils.quoteAsString(exceptionClassName, builder);
        builder.append("\",");
        builder.append("\"error.message\":\"");
        JsonUtils.quoteAsString(exceptionMessage, builder);
        builder.append("\",");
        if (stackTraceAsArray) {
            builder.append("\"error.stack_trace\":[").append(NEW_LINE);
            for (String line : stackTrace.split("\\n")) {
                appendQuoted(builder, line);
            }
            builder.append("]");
        } else {
            builder.append("\"error.stack_trace\":\"");
            JsonUtils.quoteAsString(stackTrace, builder);
            builder.append("\"");
        }
    }

    private static void appendQuoted(StringBuilder builder, CharSequence content) {
        builder.append('"');
        JsonUtils.quoteAsString(content, builder);
        builder.append('"');
    }

    private static CharSequence formatThrowable(final Throwable throwable) {
        StringBuilder buffer = getMessageStringBuilder();
        final PrintWriter pw = new PrintWriter(new StringBuilderWriter(buffer));
        throwable.printStackTrace(pw);
        pw.flush();
        return buffer;
    }

    private static void formatThrowableAsArray(final StringBuilder jsonBuilder, final Throwable throwable) {
        final StringBuilder buffer = getMessageStringBuilder();
        final PrintWriter pw = new PrintWriter(new StringBuilderWriter(buffer), true) {
            @Override
            public void println() {
                flush();
                jsonBuilder.append("\t\"");
                JsonUtils.quoteAsString(buffer, jsonBuilder);
                jsonBuilder.append("\",");
                jsonBuilder.append(NEW_LINE);
                buffer.setLength(0);
            }
        };
        throwable.printStackTrace(pw);
        removeIfEndsWith(jsonBuilder, NEW_LINE);
        removeIfEndsWith(jsonBuilder, ",");
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

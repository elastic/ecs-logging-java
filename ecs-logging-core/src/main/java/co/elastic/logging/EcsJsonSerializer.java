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

import java.util.Map;

public class EcsJsonSerializer {

    private static final TimestampSerializer TIMESTAMP_SERIALIZER = new TimestampSerializer();
    private static final ThreadLocal<StringBuilder> messageStringBuilder = new ThreadLocal<StringBuilder>();

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
        builder.append("\", ");
    }

    public static void serializeServiceName(StringBuilder builder, String serviceName) {
        if (serviceName != null) {
            builder.append("\"service.name\":\"").append(serviceName).append("\",");
        }
    }

    public static void serializeEventDataset(StringBuilder builder, String eventDataset) {
        if (eventDataset != null) {
            builder.append("\"event.dataset\":\"").append(eventDataset).append("\",");
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
        builder.append('"');
        if (lineNumber >= 0) {
            builder.append(',');
            builder.append("\"file.line\":");
            builder.append(lineNumber);
        }
        builder.append("},");
    }

    public static void serializeMDC(StringBuilder builder, Map<String, ?> properties) {
        if (!properties.isEmpty()) {
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
        serializeException(builder, thrown, stackTraceAsArray, DefaultThrowableSerializer.getInstance());
    }

    public static void serializeException(StringBuilder builder, Throwable thrown, boolean stackTraceAsArray, ThrowableSerializer throwableSerializer) {
        if (thrown != null) {
            throwableSerializer.serialize(builder, thrown, stackTraceAsArray);
        }
    }

    public static void serializeException(StringBuilder builder, String exceptionClassName, String exceptionMessage, String stackTrace, boolean stackTraceAsArray) {
        serializeException(builder, exceptionClassName, exceptionMessage, stackTrace, stackTraceAsArray, DefaultThrowableSerializer.getInstance());
    }

    public static void serializeException(StringBuilder builder, String exceptionClassName, String exceptionMessage, String stackTrace, boolean stackTraceAsArray, ThrowableSerializer throwableSerializer) {
        throwableSerializer.serialize(builder, exceptionClassName, exceptionMessage, stackTrace, stackTraceAsArray);
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
            return serviceName + ".log";
        }
        return eventDataset;
    }
}

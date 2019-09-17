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
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EcsJsonSerializer {

    public static final List<String> DEFAULT_TOP_LEVEL_LABELS = Arrays.asList("trace.id", "transaction.id", "span.id", "error.id", "service.name");
    private static final TimestampSerializer TIMESTAMP_SERIALIZER = new TimestampSerializer();

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
        // last char is always a comma (,)
        builder.setLength(builder.length() - 1);
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

    public static void serializeFormattedMessage(StringBuilder builder, String message, Throwable t) {
        builder.append("\"message\":\"");
        JsonUtils.quoteAsString(message, builder);
        if (t != null) {
            builder.append("\\n");
            JsonUtils.quoteAsString(formatThrowable(t), builder);
        }
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

    public static void serializeException(StringBuilder builder, Throwable thrown) {
        if (thrown != null) {
            builder.append("\"error.code\":\"");
            JsonUtils.quoteAsString(thrown.getClass().getName(), builder);
            builder.append("\",");
            builder.append("\"error.message\":\"");
            JsonUtils.quoteAsString(formatThrowable(thrown), builder);
            builder.append("\",");
        }
    }

    private static CharSequence formatThrowable(final Throwable throwable) {
        StringWriter sw = new StringWriter(2048);
        final PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }
}

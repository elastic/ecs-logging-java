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
import java.util.regex.Pattern;

import static co.elastic.logging.EcsJsonSerializer.getMessageStringBuilder;

public class DefaultThrowableSerializer implements ThrowableSerializer {
    static final  String NEW_LINE = System.getProperty("line.separator");
    static final Pattern NEW_LINE_PATTERN = Pattern.compile("\\n");

    private static final ThrowableSerializer instance = new DefaultThrowableSerializer();

    public static ThrowableSerializer getInstance() {
        return instance;
    }

    @Override
    public void serialize(StringBuilder builder, Throwable thrown, boolean stackTraceAsArray) {
        serializeErrorType(builder, thrown);
        serializeErrorMessage(builder, thrown);
        serializeStackTrace(builder, thrown, stackTraceAsArray);
    }

    @Override
    public void serialize(StringBuilder builder, String exceptionClassName, String exceptionMessage, String stackTrace, boolean stackTraceAsArray) {
        serializeErrorType(builder, exceptionClassName);
        serializeErrorMessage(builder, exceptionMessage);
        serializeStackTrace(builder, stackTrace, stackTraceAsArray);
    }

    protected void serializeErrorType(StringBuilder builder, Throwable thrown) {
        serializeErrorType(builder, formatErrorType(thrown));
    }

    protected String formatErrorType(Throwable thrown) {
        return thrown.getClass().getName();
    }

    protected void serializeErrorType(StringBuilder builder, String errorType) {
        builder.append("\"error.type\":\"");
        JsonUtils.quoteAsString(errorType, builder);
        builder.append("\",");
    }

    protected void serializeErrorMessage(StringBuilder builder, Throwable thrown) {
        serializeErrorMessage(builder, formatErrorMessage(thrown));
    }

    protected String formatErrorMessage(Throwable thrown) {
        return thrown.getMessage();
    }

    protected void serializeErrorMessage(StringBuilder builder, String message) {
        if (message != null) {
            builder.append("\"error.message\":\"");
            JsonUtils.quoteAsString(message, builder);
            builder.append("\",");
        }
    }

    protected void serializeStackTrace(StringBuilder builder, Throwable thrown, boolean stackTraceAsArray) {
        builder.append("\"error.stack_trace\":");
        if (stackTraceAsArray) {
            serializeStackTraceAsArray(builder, thrown);
        } else {
            serializeStackTraceAsString(builder, thrown);
        }
    }

    protected void serializeStackTraceAsArray(final StringBuilder builder, Throwable thrown) {
        builder.append('[');
        final StringBuilder buffer = getMessageStringBuilder();
        final PrintWriter pw = new PrintWriter(new StringBuilderWriter(buffer), true) {
            boolean firstElement = true;
            @Override
            public void println() {
                flush();
                if (firstElement) {
                    firstElement = false;
                } else {
                    builder.append(',');
                }
                builder.append('\"');
                JsonUtils.quoteAsString(buffer, builder);
                builder.append('\"');
                buffer.setLength(0);
            }
        };
        thrown.printStackTrace(pw);
        builder.append(']');
    }

    protected void serializeStackTraceAsString(StringBuilder builder, Throwable thrown) {
        builder.append('\"');
        JsonUtils.quoteAsString(formatStackTrace(thrown), builder);
        builder.append('\"');
    }

    protected CharSequence formatStackTrace(Throwable thrown) {
        StringBuilder buffer = getMessageStringBuilder();
        final PrintWriter pw = new PrintWriter(new StringBuilderWriter(buffer));
        thrown.printStackTrace(pw);
        pw.flush();
        return buffer;
    }

    protected void serializeStackTrace(StringBuilder builder, String stackTrace, boolean stackTraceAsArray) {
        builder.append("\"error.stack_trace\":");
        if (stackTraceAsArray) {
            serializeStackTraceAsArray(builder, stackTrace);
        } else {
            serializeStackTraceAsString(builder, stackTrace);
        }
    }

    protected void serializeStackTraceAsString(StringBuilder builder, String stackTrace) {
        builder.append('\"');
        JsonUtils.quoteAsString(stackTrace, builder);
        builder.append('\"');
    }

    protected void serializeStackTraceAsArray(StringBuilder builder, String stackTrace) {
        builder.append('[');
        boolean firstElement = true;
        for (String line : NEW_LINE_PATTERN.split(stackTrace)) {
            if (firstElement) {
                firstElement = false;
            } else {
                builder.append(',');
            }
            builder.append('\"');
            JsonUtils.quoteAsString(line, builder);
            builder.append('\"');
        }
        builder.append(']');
    }
}

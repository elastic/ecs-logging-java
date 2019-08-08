package co.elastic.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EcsJsonSerializer {

    public static final List<String> DEFAULT_TOP_LEVEL_LABELS = Arrays.asList("trace.id", "transaction.id", "span.id");

    public static CharSequence toNullSafeString(final CharSequence s) {
        return s == null ? "" : s;
    }

    public static void serializeObjectStart(StringBuilder builder, long timeMillis) {
        builder.append('{');
        builder.append("\"@timestamp\":").append(timeMillis).append(",");
    }

    public static void serializeObjectEnd(StringBuilder builder) {
        // last char is always a comma (,)
        builder.setLength(builder.length() - 1);
        builder.append('}');
        builder.append('\n');
    }

    public static void serializeLoggerName(StringBuilder builder, String loggerName) {
        builder.append("\"event.dataset\":\"");
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
        builder.append("\",");
    }

    public static void serializeServiceName(StringBuilder builder, String serviceName) {
        if (serviceName != null) {
            builder.append("\"service.name\":\"").append(serviceName).append("\",");
        }
    }

    public static void serializeLogLevel(StringBuilder builder, Object level) {
        builder.append("\"log.level\":\"").append(level).append("\",");
    }

    public static void serializeTag(StringBuilder builder, String tag) {
        if (tag != null) {
            builder.append("\"tags\":[\"").append(tag).append("\"],");
        }
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
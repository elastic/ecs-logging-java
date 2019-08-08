package co.elastic.logging.log4j;

import co.elastic.logging.EcsJsonSerializer;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

import java.util.HashSet;
import java.util.Set;

public class EcsLayout extends Layout {

    private String serviceName;
    private Set<String> topLevelLabels = new HashSet<>(EcsJsonSerializer.DEFAULT_TOP_LEVEL_LABELS);

    @Override
    public String format(LoggingEvent event) {
        StringBuilder builder = new StringBuilder();
        EcsJsonSerializer.serializeObjectStart(builder, event.getTimeStamp());
        EcsJsonSerializer.serializeServiceName(builder, serviceName);
        EcsJsonSerializer.serializeThreadName(builder, event.getThreadName());
        EcsJsonSerializer.serializeLogLevel(builder, event.getLevel());
        EcsJsonSerializer.serializeLoggerName(builder, event.getLoggerName());
        EcsJsonSerializer.serializeLabels(builder, event.getProperties(), topLevelLabels);
        EcsJsonSerializer.serializeTag(builder, event.getNDC());
        Throwable thrown = event.getThrowableInformation() != null ? event.getThrowableInformation().getThrowable() : null;
        EcsJsonSerializer.serializeFormattedMessage(builder, event.getRenderedMessage(), thrown);
        EcsJsonSerializer.serializeObjectEnd(builder);
        return builder.toString();
    }

    @Override
    public boolean ignoresThrowable() {
        return false;
    }

    @Override
    public void activateOptions() {

    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}

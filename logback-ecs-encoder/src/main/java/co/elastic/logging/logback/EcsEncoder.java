package co.elastic.logging.logback;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.EncoderBase;
import co.elastic.logging.EcsJsonSerializer;
import co.elastic.logging.JsonUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class EcsEncoder extends EncoderBase<ILoggingEvent> {

    private String serviceName;
    private ThrowableProxyConverter throwableProxyConverter;
    private Set<String> topLevelLabels = new HashSet<>(EcsJsonSerializer.DEFAULT_TOP_LEVEL_LABELS);

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public void start() {
        super.start();
        throwableProxyConverter = new ThrowableProxyConverter();
        throwableProxyConverter.start();
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        StringBuilder builder = new StringBuilder();
        EcsJsonSerializer.serializeObjectStart(builder, event.getTimeStamp());
        EcsJsonSerializer.serializeLogLevel(builder, event.getLevel().toString());
        EcsJsonSerializer.serializeFormattedMessage(builder, event.getFormattedMessage(), null);
        serializeException(event, builder);
        EcsJsonSerializer.serializeServiceName(builder, serviceName);
        EcsJsonSerializer.serializeThreadName(builder, event.getThreadName());
        EcsJsonSerializer.serializeLoggerName(builder, event.getLoggerName());
        EcsJsonSerializer.serializeLabels(builder, event.getMDCPropertyMap(), topLevelLabels);
        EcsJsonSerializer.serializeObjectEnd(builder);
        // all these allocations kinda hurt
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void serializeException(ILoggingEvent event, StringBuilder builder) {
        if (event.getThrowableProxy() != null) {
            // remove `", `
            builder.setLength(builder.length() - 3);
            builder.append("\\n");
            JsonUtils.quoteAsString(throwableProxyConverter.convert(event), builder);
            builder.append("\",");
        }
    }

    @Override
    public byte[] footerBytes() {
        return null;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}

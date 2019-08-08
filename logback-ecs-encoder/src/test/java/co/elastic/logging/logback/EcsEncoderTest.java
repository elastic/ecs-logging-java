package co.elastic.logging.logback;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import co.elastic.logging.AbstractEcsLoggingTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.MDC;

import java.io.IOException;

class EcsEncoderTest extends AbstractEcsLoggingTest {

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;
    private EcsEncoder ecsEncoder;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LoggerContext context = new LoggerContext();
        logger = context.getLogger(getClass());
        appender = new ListAppender<>();
        appender.setContext(context);
        appender.start();
        logger.addAppender(appender);
        ecsEncoder = new EcsEncoder();
        ecsEncoder.setServiceName("test");
        ecsEncoder.start();
    }

    @Override
    public void putMdc(String key, String value) {
        MDC.put(key, value);
    }

    @Override
    public void debug(String message) {
        logger.debug(message);
    }

    @Override
    public void error(String message, Throwable t) {
        logger.error(message, t);
    }

    @Override
    public JsonNode getLastLogLine() throws IOException {
        return objectMapper.readTree(ecsEncoder.encode(appender.list.get(0)));
    }
}
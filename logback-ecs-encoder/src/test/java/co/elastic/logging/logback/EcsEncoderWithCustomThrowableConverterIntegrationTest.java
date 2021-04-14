package co.elastic.logging.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class EcsEncoderWithCustomThrowableConverterIntegrationTest extends AbstractEcsEncoderTest {
    private OutputStreamAppender appender;

    @BeforeEach
    void setUp() throws JoranException {
        LoggerContext context = new LoggerContext();
        ContextInitializer contextInitializer = new ContextInitializer(context);
        contextInitializer.configureByResource(this.getClass().getResource("/logback-config-with-nop-throwable-converter.xml"));
        logger = context.getLogger("root");
        appender = (OutputStreamAppender) logger.getAppender("out");
    }

    @Override
    public JsonNode getLastLogLine() throws IOException {
        return objectMapper.readTree(appender.getBytes());
    }

    @Test
    void testLogException() throws Exception {
        error("test", new RuntimeException("test"));
        JsonNode log = getAndValidateLastLogLine();
        assertThat(log.get("log.level").textValue()).isIn("ERROR", "SEVERE");
        assertThat(log.get("error.message").textValue()).isEqualTo("test");
        assertThat(log.get("error.type").textValue()).isEqualTo(RuntimeException.class.getName());
        assertThat(log.get("error.stack_trace").textValue()).contains("");
    }
}

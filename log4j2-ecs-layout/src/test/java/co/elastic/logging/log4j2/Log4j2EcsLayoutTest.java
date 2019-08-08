package co.elastic.logging.log4j2;

import co.elastic.logging.AbstractEcsLoggingTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.BasicConfigurationFactory;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class Log4j2EcsLayoutTest extends AbstractEcsLoggingTest {

    private static ConfigurationFactory configFactory = new BasicConfigurationFactory();
    private LoggerContext ctx = LoggerContext.getContext();
    private Logger root = ctx.getRootLogger();
    private ObjectMapper objectMapper = new ObjectMapper();
    private ListAppender listAppender;

    @AfterAll
    public static void cleanupClass() {
        ConfigurationFactory.removeConfigurationFactory(configFactory);
    }

    @BeforeAll
    public static void setupClass() {
        ConfigurationFactory.setConfigurationFactory(configFactory);
        final LoggerContext ctx = LoggerContext.getContext();
        ctx.reconfigure();
    }

    @BeforeEach
    public void setUp() throws Exception {
        for (final Appender appender : root.getAppenders().values()) {
            root.removeAppender(appender);
        }
        EcsLayout ecsLayout = EcsLayout.newBuilder()
                .setConfiguration(ctx.getConfiguration())
                .setServiceName("test")
                .setGlobalLabels(new KeyValuePair[]{
                        new KeyValuePair("global_foo", "bar")
                })
                .build();

        listAppender = new ListAppender("ecs", null, ecsLayout, false, false);
        listAppender.start();
        root.addAppender(listAppender);
        root.setLevel(Level.DEBUG);
    }

    @AfterEach
    public void tearDown() throws Exception {
        ThreadContext.clearAll();
    }

    @Test
    void globalLabels() throws Exception {
        debug("test");
        assertThat(getLastLogLine().get("labels.global_foo").textValue()).isEqualTo("bar");
    }

    @Override
    public void putMdc(String key, String value) {
        ThreadContext.put(key, value);
    }

    @Override
    public boolean putNdc(String message) {
        ThreadContext.push(message);
        return true;
    }

    @Override
    public void debug(String message) {
        root.debug(message);
    }

    @Override
    public void error(String message, Throwable t) {
        root.error(message, t);
    }

    @Override
    public JsonNode getLastLogLine() throws IOException {
        return objectMapper.readTree(listAppender.getMessages().get(0));
    }
}
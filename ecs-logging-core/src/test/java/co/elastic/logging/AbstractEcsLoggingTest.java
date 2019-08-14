package co.elastic.logging;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractEcsLoggingTest {

    protected ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testMetadata() throws Exception {
        debug("test");
        assertThat(getLastLogLine().get("process.thread.name").textValue()).isEqualTo(Thread.currentThread().getName());
        assertThat(getLastLogLine().get("service.name").textValue()).isEqualTo("test");
        assertThat(getLastLogLine().get("@timestamp").longValue()).isGreaterThan(0);
        assertThat(getLastLogLine().get("log.level").textValue()).isEqualTo("DEBUG");
        assertThat(getLastLogLine().get("log.logger")).isNotNull();
    }

    @Test
    void testSimpleLog() throws Exception {
        debug("test");
        assertThat(getLastLogLine().get("message").textValue()).isEqualTo("test");
    }

    @Test
    void testThreadContext() throws Exception {
        putMdc("foo", "bar");
        debug("test");
        assertThat(getLastLogLine().get("labels.foo").textValue()).isEqualTo("bar");
    }

    @Test
    void testThreadContextStack() throws Exception {
        if (putNdc("foo")) {
            debug("test");
            assertThat(getLastLogLine().get("tags").iterator().next().textValue()).isEqualTo("foo");
        }
    }

    @Test
    void testTopLevelLabels() throws Exception {
        putMdc("transaction.id", "0af7651916cd43dd8448eb211c80319c");
        debug("test");
        assertThat(getLastLogLine().get("labels.transaction.id")).isNull();
        assertThat(getLastLogLine().get("transaction.id").textValue()).isEqualTo("0af7651916cd43dd8448eb211c80319c");
    }

    @Test
    void testLogException() throws Exception {
        error("test", new RuntimeException("test"));
        assertThat(getLastLogLine().get("log.level").textValue()).isEqualTo("ERROR");
        assertThat(getLastLogLine().get("message").textValue()).contains("at co.elastic.logging.AbstractEcsLoggingTest.testLogException");
    }

    public abstract void putMdc(String key, String value);

    public boolean putNdc(String message) {
        return false;
    }

    public abstract void debug(String message);

    public abstract void error(String message, Throwable t);

    public abstract JsonNode getLastLogLine() throws IOException;
}

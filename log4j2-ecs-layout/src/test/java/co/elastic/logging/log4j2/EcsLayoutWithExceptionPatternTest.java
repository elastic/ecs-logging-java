package co.elastic.logging.log4j2;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EcsLayoutWithExceptionPatternTest extends Log4j2EcsLayoutTest {
    @Override
    protected EcsLayout.Builder configureLayout(LoggerContext context) {
        return super.configureLayout(context)
                .setExceptionPattern("%cEx");
    }

    @Test
    void testLogException() throws Exception {
        error("test", new RuntimeException("test"));
        JsonNode log = getLastLogLine();
        assertThat(log.get("log.level").textValue()).isIn("ERROR", "SEVERE");
        assertThat(log.get("error.message").textValue()).isEqualTo("test");
        assertThat(log.get("error.type").textValue()).isEqualTo(RuntimeException.class.getName());
        assertThat(log.get("error.stack_trace").textValue()).isEqualTo("java.lang.RuntimeException: test\nSTACK_TRACE!");
    }

    @Test
    void testLogExceptionNullMessage() throws Exception {
        error("test", new RuntimeException());
        JsonNode log = getLastLogLine();;
        assertThat(log.get("error.type").textValue()).isEqualTo(RuntimeException.class.getName());
        assertThat(log.get("error.message")).isNull();
    }
}

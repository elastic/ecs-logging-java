package co.elastic.logging.log4j2;

import static co.elastic.logging.log4j2.CustomMdcSerializer.CUSTOM_MDC_SERIALIZER_TEST_KEY;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.core.LoggerContext;

import co.elastic.logging.log4j2.EcsLayout.Builder;

public class EcsLayoutWithCustomMdcSerializerTest extends Log4j2EcsLayoutTest {

    @Override
    protected Builder configureLayout(LoggerContext context) {
        return super.configureLayout(context)
                .setMdcSerializerFullClassName("co.elastic.logging.log4j2.CustomMdcSerializer");
    }

    @Override
    void testAdditionalFieldsWithLookup() throws Exception {
        putMdc("trace.id", "foo");
        putMdc("foo", "bar");
        putMdc(CUSTOM_MDC_SERIALIZER_TEST_KEY, "some_text_lower_case");
        debug("test");
        assertThat(getAndValidateLastLogLine().get("cluster.uuid").textValue()).isEqualTo("9fe9134b-20b0-465e-acf9-8cc09ac9053b");
        assertThat(getAndValidateLastLogLine().get("node.id").textValue()).isEqualTo("foo");
        assertThat(getAndValidateLastLogLine().get("empty")).isNull();
        assertThat(getAndValidateLastLogLine().get("emptyPattern")).isNull();
        assertThat(getAndValidateLastLogLine().get("clazz").textValue()).startsWith(getClass().getPackageName());
        assertThat(getAndValidateLastLogLine().get("404")).isNull();
        assertThat(getAndValidateLastLogLine().get("foo").textValue()).isEqualTo("bar");
        assertThat(getAndValidateLastLogLine().get(CUSTOM_MDC_SERIALIZER_TEST_KEY).textValue()).isEqualTo("SOME_TEXT_LOWER_CASE");
    }
}

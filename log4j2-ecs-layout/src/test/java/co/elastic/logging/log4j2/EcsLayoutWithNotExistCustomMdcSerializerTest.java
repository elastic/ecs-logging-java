package co.elastic.logging.log4j2;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EcsLayoutWithNotExistCustomMdcSerializerTest {

    @Test
    void testThrowExceptionIfMdcSerializerNotExist() {
        assertThatThrownBy(() -> EcsLayout.newBuilder()
                .setMdcSerializerFullClassName("not-exist-class-name")
                .setServiceName("test")
                .build()).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Could not create MdcSerializer not-exist-class-name");
    }

}

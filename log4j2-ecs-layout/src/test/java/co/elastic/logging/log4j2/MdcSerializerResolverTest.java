package co.elastic.logging.log4j2;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MdcSerializerResolverTest {

    @Test
    public void testResolveWithNull() {
        MdcSerializer serializer = MdcSerializerResolver.resolve(null);
        assertThat(serializer).isNotNull()
                .isInstanceOf(DefaultMdcSerializer.UsingContextMap.class);
    }

    @Test
    public void testResolveWithEmptyString() {
        MdcSerializer serializer = MdcSerializerResolver.resolve("");
        assertThat(serializer).isNotNull()
                .isInstanceOf(DefaultMdcSerializer.UsingContextMap.class);
    }

    @Test
    public void testResolveWithValidClassName() {
        String validClassName = "co.elastic.logging.log4j2.CustomMdcSerializer";
        MdcSerializer serializer = MdcSerializerResolver.resolve(validClassName);
        assertThat(serializer).isNotNull()
                .isInstanceOf(CustomMdcSerializer.class);
    }

    @Test
    public void testResolveWithInvalidClassName() {
        String invalidClassName = "co.elastic.logging.InvalidClass";
        assertThatThrownBy(() -> MdcSerializerResolver.resolve(invalidClassName)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Could not create MdcSerializer");
    }
}

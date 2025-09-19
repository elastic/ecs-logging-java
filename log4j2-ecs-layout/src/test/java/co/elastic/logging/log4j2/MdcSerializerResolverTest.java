/*-
 * #%L
 * Java ECS logging
 * %%
 * Copyright (C) 2019 - 2025 Elastic and contributors
 * %%
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * #L%
 */
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

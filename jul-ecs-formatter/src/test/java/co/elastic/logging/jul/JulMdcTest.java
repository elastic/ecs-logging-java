/*-
 * #%L
 * Java ECS logging
 * %%
 * Copyright (C) 2019 - 2022 Elastic and contributors
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
package co.elastic.logging.jul;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class JulMdcTest {

    @AfterEach
    void after() {
        JulMdc.getEntries().clear();
        assertThat(JulMdc.getEntries()).isEmpty();
    }

    @Test
    void emptyMdc() {
        Map<String, String> entries = JulMdc.getEntries();
        assertThat(entries).isEmpty();

        assertThat(JulMdc.getEntries()).isSameAs(entries);

        // should be a no-op
        JulMdc.remove("missing");
    }

    @Test
    void putRemoveSingleEntry() {
        JulMdc.put("hello", "world");
        assertThat(JulMdc.getEntries()).containsEntry("hello", "world");

        JulMdc.remove("hello");
        assertThat(JulMdc.getEntries()).isEmpty();
    }

}

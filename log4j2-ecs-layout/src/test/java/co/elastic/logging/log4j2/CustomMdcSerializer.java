/*-
 * #%L
 * Java ECS logging
 * %%
 * Copyright (C) 2019 - 2020 Elastic and contributors
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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.util.TriConsumer;

import co.elastic.logging.EcsJsonSerializer;
import co.elastic.logging.JsonUtils;

public class CustomMdcSerializer implements MdcSerializer {

    protected static final String  CUSTOM_MDC_SERIALIZER_TEST_KEY = "SPECIAL_TEST_CUSTOM_KEY";

    @Override
    public void serializeMdc(LogEvent event, StringBuilder builder) {
        event.getContextData()
                .forEach((key, value) -> getWriteFunctionForKey(key).accept(key, value, builder));
    }

    // Default function for serializing MDC entries
    private static final TriConsumer<String, Object, StringBuilder> DEFAULT_WRITE_MDC_FUNCTION = (key, value, stringBuilder) -> {
        stringBuilder.append('\"');
        JsonUtils.quoteAsString(key, stringBuilder);
        stringBuilder.append("\":\"");
        JsonUtils.quoteAsString(EcsJsonSerializer.toNullSafeString(String.valueOf(value)), stringBuilder);
        stringBuilder.append("\",");
    };

    // Custom function for handling a specific key
    private static final TriConsumer<String, Object, StringBuilder> CUSTOM_KEY_WRITE_MDC_FUNCTION = (key, value, stringBuilder) -> DEFAULT_WRITE_MDC_FUNCTION.accept(
            key,
            value.toString().toUpperCase(),
            stringBuilder
    );

    /**
     * Returns the appropriate function to write an MDC entry based on the key.
     *
     * @param key MDC key.
     * @return The function to serialize the MDC entry value.
     */
    private TriConsumer<String, Object, StringBuilder> getWriteFunctionForKey(String key) {
        if (CUSTOM_MDC_SERIALIZER_TEST_KEY.equals(key)) {
            return CUSTOM_KEY_WRITE_MDC_FUNCTION;
        }
        return DEFAULT_WRITE_MDC_FUNCTION;
    }
}

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

import co.elastic.logging.EcsJsonSerializer;
import co.elastic.logging.JsonUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.util.TriConsumer;

interface DefaultMdcSerializer extends MdcSerializer {

    /**
     * Garbage free MDC serialization for log4j2 2.7+
     * Never reference directly in prod code so avoid linkage errors when TriConsumer or getContextData are not available
     */
    enum UsingContextData implements MdcSerializer {

        @SuppressWarnings("unused")
        INSTANCE;

        private static final TriConsumer<String, Object, StringBuilder> WRITE_MDC = new TriConsumer<String, Object, StringBuilder>() {
            @Override
            public void accept(final String key, final Object value, final StringBuilder stringBuilder) {
                stringBuilder.append('\"');
                JsonUtils.quoteAsString(key, stringBuilder);
                stringBuilder.append("\":\"");
                JsonUtils.quoteAsString(EcsJsonSerializer.toNullSafeString(String.valueOf(value)), stringBuilder);
                stringBuilder.append("\",");
            }
        };


        @Override
        public void serializeMdc(LogEvent event, StringBuilder builder) {
            event.getContextData().forEach(WRITE_MDC, builder);
        }
    }

    /**
     * Fallback for log4j2 <= 2.6
     */
    enum UsingContextMap implements MdcSerializer {
        INSTANCE;

        @Override
        public void serializeMdc(LogEvent event, StringBuilder builder) {
            EcsJsonSerializer.serializeMDC(builder, event.getContextMap());
        }
    }
}

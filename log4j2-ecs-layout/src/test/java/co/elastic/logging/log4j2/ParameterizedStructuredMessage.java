/*-
 * #%L
 * Java ECS logging
 * %%
 * Copyright (C) 2019 Elastic and contributors
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

import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.TriConsumer;

public class ParameterizedStructuredMessage extends MapMessage<ParameterizedStructuredMessage, Object> {

    private static final String MESSAGE = "message";
    private static final TriConsumer<String, Object, StringBuilder> ADD_KEY_VALUE_PAIR = new TriConsumer<>() {
        @Override
        public void accept(String key, Object value, StringBuilder builder) {
            if (!MESSAGE.equals(key)) {
                builder.append(' ').append(key).append('[').append(value).append(']');
            }
        }
    };

    private final String message;

    private ParameterizedStructuredMessage(String message) {
        this.message = message;
        with(MESSAGE, message);
    }

    private ParameterizedStructuredMessage(String messagePattern, Object... arguments) {
        this.message = ParameterizedMessage.format(messagePattern, arguments);
        with(MESSAGE, message);
    }

    public static ParameterizedStructuredMessage of(String message) {
        return new ParameterizedStructuredMessage(message);
    }

    public static ParameterizedStructuredMessage of(String messagePattern, Object... arguments) {
        return new ParameterizedStructuredMessage(messagePattern, arguments);
    }

    @Override
    protected void asJson(StringBuilder sb) {
        super.asJson(sb);
    }

    protected void appendMap(final StringBuilder sb) {
        sb.append(message);
        forEach(ADD_KEY_VALUE_PAIR, sb);
    }
}

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

import org.apache.logging.log4j.message.MultiformatMessage;
import org.apache.logging.log4j.util.MultiFormatStringBuilderFormattable;

interface MultiFormatHandler {

    void formatJsonTo(MultiformatMessage message, StringBuilder builder);

    class Resolver {

        static MultiFormatHandler resolve() {
            try {
                Class.forName("org.apache.logging.log4j.util.MultiFormatStringBuilderFormattable");
                return (MultiFormatHandler) Class.forName("co.elastic.logging.log4j2.MultiFormatHandler$MultiFormatStringBuilderFormattableAware").getEnumConstants()[0];
            } catch (Exception ignore) {
            } catch (LinkageError ignore) {
            }
            return ForLegacyLog4j.INSTANCE;
        }

    }

    /**
     * For log4j2 {@code >=} 2.10
     * Never reference directly in prod code so avoid linkage errors when {@link MultiFormatStringBuilderFormattable} is not available
     */
    enum MultiFormatStringBuilderFormattableAware implements MultiFormatHandler {
        INSTANCE;
        private static final String[] JSON_FORMAT = {"JSON"};

        @Override
        public void formatJsonTo(MultiformatMessage message, StringBuilder builder) {
            if (message instanceof MultiFormatStringBuilderFormattable) {
                ((MultiFormatStringBuilderFormattable) message).formatTo(JSON_FORMAT, builder);
            } else {
                builder.append(message.getFormattedMessage(JSON_FORMAT));
            }
        }
    }

    /**
     * Fallback for log4j2 {@code <} 2.10
     */
    enum ForLegacyLog4j implements MultiFormatHandler {
        INSTANCE;
        private static final String[] JSON_FORMAT = {"JSON"};

        @Override
        public void formatJsonTo(MultiformatMessage message, StringBuilder builder) {
            builder.append(message.getFormattedMessage(JSON_FORMAT));
        }
    }

}

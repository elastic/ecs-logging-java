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
package co.elastic.logging.log4j;

import org.apache.log4j.MDC;
import org.apache.log4j.pattern.LogEvent;
import org.apache.log4j.spi.LoggingEvent;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

public interface MdcAccess {

    Map<String, ?> getMDC(LoggingEvent event);

    class Resolver {
        public static MdcAccess resolve() {
            MdcAccess access = ForLegacyLog4j.INSTANCE;
            try {
                Method getProperties = LoggingEvent.class.getMethod("getProperties");
                access = (MdcAccess) Class.forName("co.elastic.logging.log4j.MdcAccess$GetPropertiesCapable").getEnumConstants()[0];
            } catch (Exception ignore) {
            } catch (LinkageError ignore) {
            }
            return access;
        }
    }

    /**
     * For log4j versions {@code >=} 1.2.15 that have the {@link LogEvent#getProperties()} method
     */
    enum GetPropertiesCapable implements MdcAccess {
        INSTANCE;

        @Override
        public Map<String, ?> getMDC(LoggingEvent event) {
            if (Thread.currentThread().getName().equals(event.getThreadName())) {
                // avoids copying the properties if the appender is synchronous
                return MDC.getContext();
            }
            return event.getProperties();
        }
    }

    /**
     * Fallback for log4j versions {@code <} 1.2.15
     */
    enum ForLegacyLog4j implements MdcAccess {
        INSTANCE;

        @Override
        public Map<String, ?> getMDC(LoggingEvent event) {
            if (Thread.currentThread().getName().equals(event.getThreadName())) {
                return MDC.getContext();
            }
            // can't access MDC in async appenders
            return Collections.emptyMap();
        }
    }
}

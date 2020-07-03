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
package co.elastic.logging.jul;

import org.slf4j.MDC;

import java.util.Collections;
import java.util.Map;

public interface MdcSupplier {
    Map<String, String> getMDC();

    enum Resolver {
        INSTANCE;

        MdcSupplier resolve() {
            try {
                Class.forName("org.slf4j.MDC");

                // The SLF4J API dependency does not contain an MDC binder by itself, the MDC binders come from an SLF4j
                // implementation. When no MDC bindings are available calls to MDC.put will be ignored by slf4j.
                // That is why we want to ensure that the StaticMDCBinder exists
                Class.forName("org.slf4j.impl.StaticMDCBinder");
                return (MdcSupplier) Class.forName("co.elastic.logging.jul.MdcSupplier$Available").getEnumConstants()[0];
            } catch (Exception e) {
                return Unavailable.INSTANCE;
            } catch (LinkageError e ) {
                return Unavailable.INSTANCE;
            }
        }
    }

    enum Available implements MdcSupplier {
        INSTANCE;

        @Override
        public Map<String, String> getMDC() {
            Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();
            if (copyOfContextMap != null ) {
                return copyOfContextMap;
            }
            return Collections.emptyMap();
        }
    }

    enum Unavailable implements MdcSupplier {
        INSTANCE;

        Unavailable() {
        }

        @Override
        public Map<String, String> getMDC() {
            return Collections.emptyMap();
        }
    }
}

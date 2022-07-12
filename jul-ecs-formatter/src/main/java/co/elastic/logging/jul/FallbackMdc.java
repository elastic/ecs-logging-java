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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Fallback MDC implementation to be used when JUL is deployed without one
 */
public class FallbackMdc {

    public static final FallbackMdc INSTANCE = new FallbackMdc();

    private final InheritableThreadLocal<Map<String, String>> tlm = new InheritableThreadLocal<Map<String, String>>() {
        @Override
        protected Map<String, String> childValue(Map<String, String> parentValue) {
            if (parentValue == null || parentValue.isEmpty()) {
                return Collections.emptyMap();
            } else {
                return new HashMap<String, String>(parentValue);
            }
        }
    };

    FallbackMdc() {
    }

    public void put(String key, String value) {
        getOrCreateMap().put(key, value);
    }

    public Map<String, String> getEntries() {
        return tlm.get();
    }

    private Map<String, String> getOrCreateMap() {
        Map<String, String> map = tlm.get();
        if (map == null || map.isEmpty()) {
            map = new HashMap<String, String>();
            tlm.set(map);
        }
        return map;
    }


}

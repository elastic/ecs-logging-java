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

import java.lang.reflect.InvocationTargetException;

import org.apache.logging.log4j.core.LogEvent;

import co.elastic.logging.log4j2.DefaultMdcSerializer.UsingContextMap;

class MdcSerializerResolver {

    static MdcSerializer resolve(String mdcSerializerFullClassName) {
        if (mdcSerializerFullClassName == null || mdcSerializerFullClassName.isEmpty()) {
            return resolveDefault();
        }
        try {
            Class<?> clazz = Class.forName(mdcSerializerFullClassName);
            return (MdcSerializer) clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
            throw new IllegalArgumentException("Could not create MdcSerializer " + mdcSerializerFullClassName, e);
        }
    }

    private static MdcSerializer resolveDefault() {
        try {
            LogEvent.class.getMethod("getContextData");
            return (DefaultMdcSerializer) Class.forName(
                    "co.elastic.logging.log4j2.DefaultMdcSerializer$UsingContextData").getEnumConstants()[0];
        } catch (Exception | LinkageError ignore) {
        }
        return UsingContextMap.INSTANCE;
    }

}

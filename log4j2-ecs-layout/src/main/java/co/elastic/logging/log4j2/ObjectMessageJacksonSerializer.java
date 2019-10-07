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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.logging.log4j.core.util.StringBuilderWriter;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.IOException;
import java.util.ServiceLoader;

interface ObjectMessageJacksonSerializer {

    void formatTo(StringBuilder buffer, ObjectMessage objectMessage);

    enum Resolver {
        INSTANCE;

        ObjectMessageJacksonSerializer resolve() {
            ObjectMessageJacksonSerializer localDelegate = null;
            try {
                // safely discovers if Jackson is available
                Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
                // this method has been introduced in 2.7
                Class.forName("org.apache.logging.log4j.message.ObjectMessage").getMethod("getParameter");
                // avoid initializing ObjectMessageSerializer$WithJackson if Jackson is not on the classpath to avoid linkage errors
                return  (ObjectMessageJacksonSerializer) Class.forName("co.elastic.logging.log4j2.ObjectMessageJacksonSerializer$Available").getEnumConstants()[0];
            } catch (Exception e) {
                return null;
            } catch (LinkageError e) {
                // we should not cause linkage errors but just in case...
                return null;
            }
        }
    }

    enum Available implements ObjectMessageJacksonSerializer {
        INSTANCE;

        private final ObjectMapper objectMapper;

        Available() {
            ObjectMapper mapper = null;
            for (ObjectMapperFactory objectMapperFactory : ServiceLoader.load(ObjectMapperFactory.class, ObjectMessageJacksonSerializer.class.getClassLoader())) {
                mapper = objectMapperFactory.create();
                break;
            }
            if (mapper == null) {
                mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS).findAndRegisterModules();
            }
            objectMapper = mapper;
        }

        @Override
        public void formatTo(StringBuilder buffer, ObjectMessage objectMessage) {
            try {
                objectMapper.writeValue(new StringBuilderWriter(buffer), objectMessage.getParameter());
            } catch (IOException e) {
                StatusLogger.getLogger().catching(e);
                objectMessage.formatTo(buffer);
            }
        }
    }
}

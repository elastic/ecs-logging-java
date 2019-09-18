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
package co.elastic.logging.log4j;

import co.elastic.logging.EcsJsonSerializer;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import java.util.HashSet;
import java.util.Set;

public class EcsLayout extends Layout {

    private boolean stackTraceAsArray = false;
    private String serviceName;
    private Set<String> topLevelLabels = new HashSet<String>(EcsJsonSerializer.DEFAULT_TOP_LEVEL_LABELS);

    @Override
    public String format(LoggingEvent event) {
        StringBuilder builder = new StringBuilder();
        EcsJsonSerializer.serializeObjectStart(builder, event.getTimeStamp());
        EcsJsonSerializer.serializeLogLevel(builder, event.getLevel().toString());
        EcsJsonSerializer.serializeFormattedMessage(builder, event.getRenderedMessage());
        EcsJsonSerializer.serializeServiceName(builder, serviceName);
        EcsJsonSerializer.serializeThreadName(builder, event.getThreadName());
        EcsJsonSerializer.serializeLoggerName(builder, event.getLoggerName());
        EcsJsonSerializer.serializeLabels(builder, event.getProperties(), topLevelLabels);
        EcsJsonSerializer.serializeTag(builder, event.getNDC());
        ThrowableInformation throwableInformation = event.getThrowableInformation();
        if (throwableInformation != null) {
            EcsJsonSerializer.serializeException(builder, throwableInformation.getThrowable(), stackTraceAsArray);
        }
        EcsJsonSerializer.serializeObjectEnd(builder);
        return builder.toString();
    }

    @Override
    public boolean ignoresThrowable() {
        return false;
    }

    @Override
    public void activateOptions() {

    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setStackTraceAsArray(boolean stackTraceAsArray) {
        this.stackTraceAsArray = stackTraceAsArray;
    }
}

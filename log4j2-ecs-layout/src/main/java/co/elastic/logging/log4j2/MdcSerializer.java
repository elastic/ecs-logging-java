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

/**
 * Interface for serializing MDC (Mapped Diagnostic Context) data from a {@link LogEvent}.
 * <p>
 * Implementations must have a public no-argument constructor to allow dynamic instantiation.
 * </p>
 */
public interface MdcSerializer {
    /**
     * Add MDC data for the give log event to the provided output string builder. The output written to the string
     * builder must be a valid JSON-object without the surrounding curly braces and with a trailing comma. If this MDC
     * serializer does not append any content, no comma shall be added. For example, the serializer could output the
     * following content: "foo":"bar","key":"value",
     *
     * @param event   the log event to write the MDC content for
     * @param builder the output JSON string builder
     */
    void serializeMdc(LogEvent event, StringBuilder builder);
}

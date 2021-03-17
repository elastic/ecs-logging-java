/*-
 * #%L
 * Java ECS logging
 * %%
 * Copyright (C) 2019 - 2021 Elastic and contributors
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
package co.elastic.logging;

import java.util.Locale;

/**
 * Based on https://github.com/elastic/ecs/blob/master/rfcs/text/0009-data_stream-fields.md#restrictions-on-values and
 * https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-create-index.html#indices-create-api-path-params
 */
public class DataStreamFieldSanitizer {

    private static final char[] DISALLOWED_IN_DATASET = new char[]{'\\', '/', '*', '?', '\"', '<', '>', '|', ' ', ',', '#', ':', '-'};
    private static final char[] DISALLOWED_IN_NAMESPACE = new char[]{'\\', '/', '*', '?', '\"', '<', '>', '|', ' ', ',', '#', ':'};
    private static final int MAX_LENGTH = 100;
    private static final char REPLACEMENT_CHAR = '_';

    public static String sanitizeDataStreamDataset(String dataset) {
        return sanitazeDataStreamField(dataset, DISALLOWED_IN_DATASET);
    }

    public static String sanitizeDataStreamNamespace(String dataStreamNamespace) {
        return sanitazeDataStreamField(dataStreamNamespace, DISALLOWED_IN_NAMESPACE);
    }

    private static String sanitazeDataStreamField(String dataset, char[] disallowedInDataset) {
        if (dataset == null || dataset.isEmpty()) {
            return dataset;
        }
        dataset = dataset.toLowerCase(Locale.ROOT)
                .substring(0, Math.min(dataset.length(), MAX_LENGTH));
        for (char c : disallowedInDataset) {
            dataset = dataset.replace(c, REPLACEMENT_CHAR);
        }
        return dataset;
    }
}

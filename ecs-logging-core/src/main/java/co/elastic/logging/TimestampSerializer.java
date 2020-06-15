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
package co.elastic.logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This class serializes an epoch timestamp in milliseconds to a ISO 8601 date time sting,
 * for example {@code 1970-01-01T00:00:00.000Z}
 * <p>
 * The main advantage of this class is that is able to serialize the timestamp in a garbage free way,
 * i.e. without object allocations and that it is faster than {@link java.text.DateFormat#format(Date)}.
 * </p>
 * <p>
 * The most complex part when formatting a ISO date is to determine the actual year,
 * month and date as you have to account for leap years.
 * Leveraging the fact that for a whole day this stays the same
 * and that logging only requires to serialize the current timestamp and not arbitrary ones,
 * we offload this task to {@link java.text.DateFormat#format(Date)} and cache the result.
 * So we only have to serialize the time part of the ISO timestamp which is easy
 * as a day has exactly {@code 1000 * 60 * 60 * 24} milliseconds.
 * Also, we don't have to worry about leap seconds when dealing with the epoch timestamp.
 * </p>
 * <p>
 * This class is thread safe.
 * </p>
 */
class TimestampSerializer {

    private static final long MILLIS_PER_SECOND = 1000;
    private static final long MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60;
    private static final long MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60;
    private static final long MILLIS_PER_DAY = MILLIS_PER_HOUR * 24;
    private static final char TIME_SEPARATOR = 'T';
    private static final char TIME_ZONE_SEPARATOR = 'Z';
    private static final char COLON = ':';
    private static final char DOT = '.';
    private static final char ZERO = '0';

    private volatile CachedDate cachedDate = new CachedDate(System.currentTimeMillis());

    void serializeEpochTimestampAsIsoDateTime(StringBuilder builder, long epochTimestamp) {
        CachedDate cachedDateLocal = cachedDate;
        if (cachedDateLocal == null || !cachedDateLocal.isDateCached(epochTimestamp)) {
            cachedDate = cachedDateLocal = new CachedDate(epochTimestamp);
        }
        builder.append(cachedDateLocal.getCachedDateIso());

        builder.append(TIME_SEPARATOR);

        // hours
        long remainder = epochTimestamp % MILLIS_PER_DAY;
        serializeWithLeadingZero(builder, remainder / MILLIS_PER_HOUR, 2);
        builder.append(COLON);

        // minutes
        remainder %= MILLIS_PER_HOUR;
        serializeWithLeadingZero(builder, remainder / MILLIS_PER_MINUTE, 2);
        builder.append(COLON);

        // seconds
        remainder %= MILLIS_PER_MINUTE;
        serializeWithLeadingZero(builder, remainder / MILLIS_PER_SECOND, 2);
        builder.append(DOT);

        // milliseconds
        remainder %= MILLIS_PER_SECOND;
        serializeWithLeadingZero(builder, remainder, 3);

        builder.append(TIME_ZONE_SEPARATOR);
    }

    private void serializeWithLeadingZero(StringBuilder builder, long value, int minLength) {
        for (int i = minLength - 1; i > 0; i--) {
            if (value < Math.pow(10, i)) {
                builder.append(ZERO);
            }
        }
        builder.append(value);
    }

    private static class CachedDate {
        private final String cachedDateIso;
        private final long startOfCachedDate;
        private final long endOfCachedDate;

        private CachedDate(long epochTimestamp) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            cachedDateIso = dateFormat.format(new Date(epochTimestamp));
            startOfCachedDate = atStartOfDay(epochTimestamp);
            endOfCachedDate = atEndOfDay(epochTimestamp);
        }

        private static long atStartOfDay(long epochTimestamp) {
            return epochTimestamp - epochTimestamp % MILLIS_PER_DAY;
        }

        private static long atEndOfDay(long epochTimestamp) {
            return atStartOfDay(epochTimestamp) + MILLIS_PER_DAY - 1;
        }

        private boolean isDateCached(long epochTimestamp) {
            return epochTimestamp >= startOfCachedDate && epochTimestamp <= endOfCachedDate;
        }

        public String getCachedDateIso() {
            return cachedDateIso;
        }
    }

}


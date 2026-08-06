/*
 * Copyright ETH 2022 - 2026 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.systemsx.cisd.authentication;


import ch.systemsx.cisd.common.security.TokenGenerator;

public class SessionTokenHash
{
    private static final char SESSION_TOKEN_SEPARATOR = '-';

    // should be different than SESSION_TOKEN_SEPARATOR
    private static final char TIMESTAMP_TOKEN_SEPARATOR = 'x';

    private static final TokenGenerator generator = new TokenGenerator();

    private final String hash;

    private SessionTokenHash(String hash)
    {
        this.hash = hash;
    }

    public static SessionTokenHash create(String user, long timestamp)
    {
        return new SessionTokenHash(user + SESSION_TOKEN_SEPARATOR + generator.getNewToken(timestamp, TIMESTAMP_TOKEN_SEPARATOR));
    }

    public static boolean isValid(String hashOrNull)
    {
        if (hashOrNull == null)
        {
            return false;
        }
        // Obtain the start of the Timestamp
        int startOfTimestampIndex = hashOrNull.lastIndexOf(SESSION_TOKEN_SEPARATOR);
        if (startOfTimestampIndex == -1) {
            return false;
        }
        // Obtain the start of the random
        int startOfRandomIndex = hashOrNull.lastIndexOf(TIMESTAMP_TOKEN_SEPARATOR);
        if (startOfRandomIndex == -1) {
            return false;
        }

        final String timestamp = hashOrNull.substring(startOfTimestampIndex + 1, startOfRandomIndex);
        final String random = hashOrNull.substring(startOfRandomIndex + 1);

        try
        {
            Long.parseLong(timestamp);
        } catch (NumberFormatException ex)
        {
            return false;
        }

        return random.length() == 32;
    }

    @Override public String toString()
    {
        return hash;
    }
}

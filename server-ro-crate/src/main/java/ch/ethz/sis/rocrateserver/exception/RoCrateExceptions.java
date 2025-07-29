/*
 * Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.rocrateserver.exception;

import jakarta.ws.rs.WebApplicationException;

import java.util.List;

import static ch.ethz.sis.rocrateserver.exception.ExceptionType.UserUsageError;


public enum RoCrateExceptions {
    // DefaultAuthenticator
    UNAVAILABLE_API_KEY(List.of(UserUsageError), 90001, 400, "Unavailable api-key"),
    SCHEMA_VALIDATION_FAILED(List.of(UserUsageError), 90002, 400, "Schema validation failed"),

    NO_RESULTS_FOUND(List.of(UserUsageError), 90003, 404, "No results found");


    private List<ExceptionType> types;
    private int code;
    private int httpCode;
    private String messageTemplate;

    RoCrateExceptions(List<ExceptionType> types, int code, int httpCode, String messageTemplate) {
        this.types = types;
        this.code = code;
        this.httpCode = httpCode;
        this.messageTemplate = messageTemplate;
    }

    public static void throwInstance(RoCrateExceptions exception, Object... args) {
        String message = exception.code + " " + String.format(exception.messageTemplate, args);
        throw new WebApplicationException(message, exception.httpCode);
    }

}

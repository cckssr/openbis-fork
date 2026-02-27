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
package ch.ethz.sis.libhttp.http;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.io.InputStream;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class HttpResponse {

    // Status Field Values

    public static final int OK = 200;
    public static final int BAD_REQUEST = 400;
    public static final int NOT_FOUND =  404;
    public static final int INTERNAL_SERVER_ERROR = 500;

    // Content Type Header and Values

    public static final String CONTENT_TYPE_HEADER = "content-type";

    public static final String CONTENT_TYPE_TEXT = "text/plain";
    public static final String CONTENT_TYPE_BINARY_DATA = "application/octet-stream";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_ZIP = "application/zip";

    // Content Disposition Header and Values

    public static final String CONTENT_DISPOSITION_HEADER = "content-disposition";
    public static final String CONTENT_DISPOSITION_VALUE = "attachment; filename=\"download.zip\"";

    //
    //
    //

    private final int status;
    private final Map<String, String> headers;
    private final InputStream input;
}

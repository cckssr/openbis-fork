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
package ch.ethz.sis.afsserver.server.impl;

import static io.netty.handler.codec.http.HttpMethod.DELETE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.POST;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import ch.ethz.sis.afsapi.dto.Chunk;
import ch.ethz.sis.afsapi.dto.DTO;
import ch.ethz.sis.afsclient.client.ChunkEncoderDecoder;
import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.afsserver.http.HttpResponse;
import ch.ethz.sis.afsserver.server.APIServer;
import ch.ethz.sis.afsserver.server.Request;
import ch.ethz.sis.afsserver.server.Response;
import ch.ethz.sis.afsserver.server.performance.PerformanceAuditor;
import ch.ethz.sis.shared.log.LogManager;
import ch.ethz.sis.shared.log.Logger;
import io.netty.handler.codec.http.HttpMethod;

/*
 * This class is supposed to be called by a TCP or HTTP transport class
 */
public class ApiServerAdapter<CONNECTION, API> extends AbstractAdapter<CONNECTION, API>
{

    private static final Logger logger = LogManager.getLogger(ApiServerAdapter.class);

    private final APIServer<CONNECTION, Request, Response, API> server;

    private final JsonObjectMapper jsonObjectMapper;

    private final ApiResponseBuilder apiResponseBuilder;

    public ApiServerAdapter(APIServer<CONNECTION, Request, Response, API> server, JsonObjectMapper jsonObjectMapper)
    {
        super(server, jsonObjectMapper);
        this.server = server;
        this.jsonObjectMapper = jsonObjectMapper;
        this.apiResponseBuilder = new ApiResponseBuilder();
    }

    @Override
    protected boolean isValidHttpMethod(final HttpMethod givenMethod, final String apiMethod)
    {
        final HttpMethod correctMethod = getHttpMethod(apiMethod);
        return correctMethod == givenMethod;
    }

    public static HttpMethod getHttpMethod(String apiMethod)
    {
        switch (apiMethod)
        {
            case "list":
            case "read":
            case "free":
            case "isSessionValid":
                return GET; // all parameters from GET methods come on the query string
            case "create":
            case "write":
            case "move":
            case "copy":
            case "login":
            case "logout":
            case "begin":
            case "prepare":
            case "commit":
            case "rollback":
            case "recover":
                return POST; // all parameters from POST methods come on the body
            case "delete":
                return HttpMethod.DELETE; // all parameters from DELETE methods come on the body
        }
        throw new UnsupportedOperationException(String.format("This line SHOULD be unreachable! apiMethod=\"%s\"", apiMethod));
    }

    @Override
    protected boolean isValidHttpMethod(final HttpMethod givenMethod)
    {
        return givenMethod == HttpMethod.GET || givenMethod == HttpMethod.POST || givenMethod == DELETE;
    }

    @Override
    protected void parseParameters(final String key, final List<String> values, final Map<String, Object> parsedParameters)
    {
        final String value = getFirst(values);
        switch (key)
        {
            case "directory":
                // Fall though
            case "recursively":
                parsedParameters.put(key, Boolean.valueOf(value));
                break;
            case "offset":
                parsedParameters.put(key, Long.valueOf(value));
                break;
            case "limit":
                parsedParameters.put(key, Integer.valueOf(value));
                break;
           default:
                parsedParameters.put(key, value);
                break;
        }
    }

    @Override
    protected HttpResponse process(final String method, final Map<String, Object> parsedParameters, final String sessionToken,
            final String interactiveSessionKey, final String transactionManagerKey, final PerformanceAuditor performanceAuditor) throws Exception
    {
        final ApiRequest apiRequest = new ApiRequest("1", method, parsedParameters, sessionToken, interactiveSessionKey, transactionManagerKey);
        final Response response = server.processOperation(apiRequest, apiResponseBuilder, performanceAuditor);
        final HttpResponse httpResponse = getHTTPResponse(response);
        return httpResponse;
    }

    @Override
    public String getPath()
    {
        return "api";
    }

}
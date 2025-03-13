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

import static ch.ethz.sis.afsserver.exception.HTTPExceptions.INVALID_PARAMETERS;
import static ch.ethz.sis.afsserver.exception.HTTPExceptions.throwInstance;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.POST;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ch.ethz.sis.afsapi.dto.Chunk;
import ch.ethz.sis.afsapi.dto.DTO;
import ch.ethz.sis.afsclient.client.ChunkEncoderDecoder;
import ch.ethz.sis.afsjson.JsonObjectMapper;
import ch.ethz.sis.afsserver.exception.HTTPExceptions;
import ch.ethz.sis.afsserver.http.HttpResponse;
import ch.ethz.sis.afsserver.http.HttpServerHandler;
import ch.ethz.sis.afsserver.http.impl.NettyHttpHandler;
import ch.ethz.sis.afsserver.server.APIServer;
import ch.ethz.sis.afsserver.server.APIServerException;
import ch.ethz.sis.afsserver.server.Request;
import ch.ethz.sis.afsserver.server.Response;
import ch.ethz.sis.afsserver.server.performance.Event;
import ch.ethz.sis.afsserver.server.performance.PerformanceAuditor;
import ch.ethz.sis.shared.log.LogManager;
import ch.ethz.sis.shared.log.Logger;
import io.netty.handler.codec.http.HttpMethod;

/*
 * This class is supposed to be called by a TCP or HTTP transport class
 */
public abstract class AbstractAdapter<CONNECTION, API> implements HttpServerHandler
{

    private static final Logger logger = LogManager.getLogger(AbstractAdapter.class);

    protected final APIServer<CONNECTION, Request, Response, API> server;

    protected final JsonObjectMapper jsonObjectMapper;

    public AbstractAdapter(
            APIServer<CONNECTION, Request, Response, API> server,
            JsonObjectMapper jsonObjectMapper)
    {
        this.server = server;
        this.jsonObjectMapper = jsonObjectMapper;
    }

    protected abstract boolean isValidHttpMethod(final HttpMethod givenMethod, final String apiMethod);

    protected abstract boolean isValidHttpMethod(final HttpMethod givenMethod);

    protected abstract void parseParameters(final String key, final List<String> values, final Map<String, Object> parsedParameters);

    protected abstract HttpResponse process(final String method, final Map<String, Object> parsedParameters, final String sessionToken,
            final String interactiveSessionKey, final String transactionManagerKey, final PerformanceAuditor performanceAuditor) throws Exception;

    public HttpResponse process(HttpMethod httpMethod, Map<String, List<String>> parameters,
            byte[] requestBody)
    {
        try
        {
            logger.traceAccess(null);
            PerformanceAuditor performanceAuditor = new PerformanceAuditor();

            if (!isValidHttpMethod(httpMethod))
            {
                return getHTTPResponse(new ApiResponse("1", null,
                        HTTPExceptions.INVALID_HTTP_METHOD.getCause()));
            }

            String method = null;
            String sessionToken = null;
            String interactiveSessionKey = null;
            String transactionManagerKey = null;
            Map<String, Object> parsedParameters = new HashMap<>();

            if(requestBody != null  && !GET.equals(httpMethod) && !isWriteMethod(httpMethod, parameters)) {
                parameters = NettyHttpHandler.getBodyParameters(requestBody);
                requestBody = null;
            }

            if(requestBody != null && requestBody.length > 0){
                parsedParameters.put("chunks", ChunkEncoderDecoder.decodeChunks(requestBody));
            }

            for (Map.Entry<String, List<String>> entry : parameters.entrySet())
            {
                try
                {
                    switch (entry.getKey())
                    {
                        case "method":
                            method = getParameter(parameters, entry.getKey());
                            if (!isValidHttpMethod(httpMethod, method))
                            {
                                return getHTTPResponse(new ApiResponse("1", null,
                                        HTTPExceptions.INVALID_HTTP_METHOD.getCause()));
                            }
                            break;
                        case "sessionToken":
                            sessionToken = getParameter(parameters, entry.getKey());
                            break;
                        case "interactiveSessionKey":
                            interactiveSessionKey = getParameter(parameters, entry.getKey());
                            break;
                        case "transactionManagerKey":
                            transactionManagerKey = getParameter(parameters, entry.getKey());
                            break;
                        case "transactionId":
                            parsedParameters.put(entry.getKey(), UUID.fromString(getParameter(parameters, entry.getKey())));
                            break;
                        default:
                            parseParameters(entry.getKey(), entry.getValue(), parsedParameters);
                            break;
                    }
                } catch (Exception e)
                {
                    logger.catching(e);
                    return getHTTPResponse(new ApiResponse("1", null,
                            INVALID_PARAMETERS.getCause(
                                    e.getClass().getSimpleName(),
                                    e.getMessage())));
                }
            }

            final HttpResponse httpResponse =
                    process(method, parsedParameters, sessionToken, interactiveSessionKey, transactionManagerKey, performanceAuditor);
            performanceAuditor.audit(Event.WriteResponse);
            logger.traceExit(performanceAuditor);
            logger.traceExit(httpResponse);
            return httpResponse;
        } catch (APIServerException e)
        {
            logger.catching(e);
            switch (e.getType())
            {
                case MethodNotFound:
                case IncorrectParameters:
                case InternalError:
                    try
                    {
                        return getHTTPResponse(new ApiResponse("1", null, e.getData()));
                    } catch (Exception ex)
                    {
                        logger.catching(ex);
                    }
            }
        } catch (Exception e)
        {
            logger.catching(e);
            try
            {
                return getHTTPResponse(new ApiResponse("1", null,
                        HTTPExceptions.UNKNOWN.getCause(e.getClass().getSimpleName(),
                                e.getMessage())));
            } catch (Exception ex)
            {
                logger.catching(ex);
            }
        }
        return null; // This should never happen, it would mean an error writing the Unknown error happened.
    }

    private boolean isWriteMethod(HttpMethod requestMethod, Map<String, List<String>> parameters) {
        return POST.equals(requestMethod) &&  parameters != null && parameters.get("method") != null &&
                !parameters.get("method").isEmpty() && "write".equals(parameters.get("method").get(0));
    }

    protected String getParameter(Map<String, List<String>> parameters, String name) {
        return getFirst(parameters.get(name));
    }

    protected <E> E getFirst(List<E> list) {
        if (list != null) {
            if (list.size() == 1)
            {
                return list.get(0);
            } else
            {
                throwInstance(INVALID_PARAMETERS);
                return null; // unreachable
            }
        } else {
            return null;
        }
    }

    public HttpResponse getHTTPResponse(Response response)
            throws Exception
    {
        boolean error = response.getError() != null;
        String contentType = null;
        byte[] body = null;
        if (error) {
            contentType = HttpResponse.CONTENT_TYPE_JSON;
            body = jsonObjectMapper.writeValue(response);
        } else
        {
            final Object result = response.getResult();
            if (result instanceof List || result instanceof DTO) {
                contentType = HttpResponse.CONTENT_TYPE_JSON;
                body = jsonObjectMapper.writeValue(response);
            } else if (result instanceof Chunk[]) {
                contentType = HttpResponse.CONTENT_TYPE_BINARY_DATA;
                body = ChunkEncoderDecoder.encodeChunksAsBytes((Chunk[]) result);
            } else {
                contentType = HttpResponse.CONTENT_TYPE_TEXT;
                body = String.valueOf(result).getBytes(StandardCharsets.UTF_8);
            }
        }
        return new HttpResponse((error)? HttpResponse.BAD_REQUEST :HttpResponse.OK, Map.of(HttpResponse.CONTENT_TYPE_HEADER ,contentType),
                new ByteArrayInputStream(body));
    }

}